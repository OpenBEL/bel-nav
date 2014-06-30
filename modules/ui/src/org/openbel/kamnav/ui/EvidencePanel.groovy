package org.openbel.kamnav.ui

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.DefaultEventTableModel
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.jdesktop.swingx.JXHyperlink
import org.jdesktop.swingx.JXTable

import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import java.awt.*
import java.util.List

import static java.awt.GridBagConstraints.*
import static javax.swing.ListSelectionModel.SINGLE_SELECTION
import static org.openbel.kamnav.common.util.Util.createColumn

class EvidencePanel implements EdgeUpdateable {

    final SwingBuilder swing
    final Expando cyr
    final JPanel panel

    JLabel citationName
    JXHyperlink citationLink
    JXTable stmtTable
    JTextPane annotationPane
    List statements
    List annotations
    JCheckBox linkChk

    EvidencePanel(SwingBuilder swing, Expando cyr) {
        this.swing = swing
        this.cyr = cyr
        statements = new BasicEventList()
        annotations = new BasicEventList()
        def stmtModel = new DefaultEventTableModel(statements,
            [
                getColumnCount: {1},
                getColumnName: {'Statement'},
                getColumnValue: {o, i -> o}
            ] as TableFormat)
        panel = swing.panel(preferredSize: [400, 400]) {
            gridBagLayout()

            label(text: 'Statements', constraints: gbc(anchor: LINE_START,
                    gridx: 0, gridy: 0,
                    gridwidth: 1, gridheight: 1,
                    weightx: 0.0, weighty: 0.1,
                    insets: [0, 0, 0, 0]))
            scrollPane(constraints: gbc(
                    anchor: LINE_START, gridx: 0, gridy: 1, gridwidth: 1, gridheight: 1,
                    weightx: 1.0, weighty: 0.50, insets: [0, 5, 0, 5], fill: BOTH)) {
                stmtTable = jxTable(model: stmtModel, columnControlVisible: false)
                stmtTable.selectionMode = SINGLE_SELECTION
                stmtTable.selectionModel.addListSelectionListener(
                    [
                        valueChanged: {ListSelectionEvent e ->
                            if (!e.valueIsAdjusting) {
                                def selection = stmtTable.selectedRows.
                                        collect(stmtTable.&convertRowIndexToModel).
                                        collect {statements.get(it)}
                                edt {
                                    if (!selection) {
                                        citationName.text = ''
                                        citationLink.text = ''
                                        annotations.removeAll {true}
                                        return
                                    }
                                    def ev = selection.first().ev
                                    def type = ev.citation.type
                                    def ref = ev.citation.reference
                                    def name = ev.citation.name
                                    citationName.text = name
                                    citationLink.text = ref ?: ''
                                    citationLink.URI = makeCitationURI(type, ref)

                                    def html = "<html><table width=\"100%\" height=\"100%\">"
                                    ev.biological_context.
                                        sort { it.key }.
                                        findAll {it.value}.each {
                                            html += "<tr valign=\"top\"><td>${it.key}</td><td>${it.value}</td></tr>"
                                        }
                                    html += "</table></html>"
                                    annotationPane.text = html
                                    annotationPane.caretPosition = 0

                                    def cyN = cyr.cyNetworkManager.getNetwork(ev.network)
                                    def cyE = cyN.getEdge(ev.edge)
                                    linkChk.selected = evidenceAdded(makeEvidenceValue(ev), cyN, cyE)
                                }
                            }
                        }
                    ] as ListSelectionListener
                )
            }

            label(text: 'Citation', constraints: gbc(anchor: LINE_START,
                    gridx: 0, gridy: 2, gridwidth: 1, gridheight: 1,
                    weightx: 1.0, weighty: 0.1, insets: [0, 0, 0, 0]))
            panel(constraints: gbc(anchor: LINE_START, gridx: 0, gridy: 3,
                    gridwidth: 1, gridheight: 1, weightx: 1.0, weighty: 0.1,
                    insets: [0, 20, 0, 0])) {
                gridBagLayout()

                label(text: 'Name:', constraints: gbc(anchor: LINE_START,
                        gridx: 0, gridy: 0, gridwidth: 1, gridheight: 1,
                        weightx: 0.20, weighty: 0.5))
                citationName = label(text: '', constraints: gbc(anchor: LINE_START,
                        gridx: 1, gridy: 0, gridwidth: 1, gridheight: 1,
                        weightx: 0.80, weighty: 0.50, insets: [0, 10, 0, 0]))
                label(text: 'Link:', constraints: gbc(anchor: LINE_START,
                        gridx: 0, gridy: 1, gridwidth: 1, gridheight: 1,
                        weightx: 0.20, weighty: 0.5))
                citationLink = jxHyperlink(text: '', constraints: gbc(anchor: LINE_START,
                        gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                        weightx: 0.80, weighty: 0.50, insets: [0, 10, 0, 0]))
            }

            label(text: 'Annotations', constraints: gbc(anchor: LINE_START,
                    gridx: 0, gridy: 4, gridwidth: 1, gridheight: 1,
                    weightx: 1.0, weighty: 0.1, insets: [0, 0, 0, 0]))
            scrollPane(constraints: gbc(anchor: LINE_START, gridx: 0, gridy: 5,
                            gridwidth: 1, gridheight: 1, weightx: 1.0, weighty: 0.5,
                            insets: [0, 5, 5, 5], fill: BOTH)) {
                annotationPane = textPane(contentType: "text/html", background: null,
                        border: null, editable: false)
            }
            panel(constraints: gbc(anchor: LINE_END, gridx: 0, gridy: 6, gridwidth: 1, weightx: 1.0, weighty: 0.1, fill: BOTH)) {
                flowLayout(alignment: FlowLayout.RIGHT)
                linkChk = checkBox(name: 'linkChk', action: action(name: 'Add to Edge table?', mnemonic: 'A', closure: {
                    def selection = stmtTable.selectedRows.
                            collect(stmtTable.&convertRowIndexToModel).
                            collect {statements.get(it)}
                    def modelEvidence = selection.first().ev
                    CyNetwork cyN = cyr.cyNetworkManager.getNetwork(modelEvidence.network)
                    def evTbl = cyr.cyTableManager.getAllTables(true).find { it.title == 'BEL.Evidence' }
                    if (!evTbl) return

                    def row = cyN.getRow(cyN.getEdge(modelEvidence.edge))
                    createColumn(cyN.defaultEdgeTable, 'evidence', String.class, false, null)
                    def canonical = makeEvidenceValue(modelEvidence)
                    def columnEvidence = row.get('evidence', String.class) ?
                        new JsonSlurper().parseText(row.get('evidence', String.class)) : []

                    def withChange = [columnEvidence].flatten()
                    if (linkChk.selected) {
                        // treated like set
                        if (!(canonical in columnEvidence)) {
                            withChange.add(canonical)
                        }
                    } else {
                        withChange.remove(canonical)
                    }
                    row.set('evidence',
                            (withChange ?
                                new JsonBuilder(withChange).toString() : null))
                }))
            }
        }
    }

    def update(evidence) {
        swing.doLater {
            // clear citation
            citationName.text = ''
            citationLink.text = ''

            // clear annotations
            annotations.removeAll {true}

            // update statement table
            statements.removeAll {true}
            statements.addAll(evidence.collect {
                new Expando(stmt: it.bel_statement, ev: it, toString: {stmt})
            }.sort {it.stmt})

            if (stmtTable.getRowCount())
                stmtTable.selectionModel.setSelectionInterval(0, 0)
        }
    }

    static Map makeEvidenceValue(Map val) {
        val.biological_context = val.biological_context.findAll { it.value }.sort()
        val.subMap('bel_statement', 'citation', 'biological_context')
    }

    static boolean evidenceAdded(Map evidence, CyNetwork cyN, CyEdge edge) {
        def row = cyN.getRow(edge)
        createColumn(cyN.defaultEdgeTable, 'evidence', String.class, false, null)
        def canonical = makeEvidenceValue(evidence)
        def columnEvidence = row.get('evidence', String.class) ?
            new JsonSlurper().parseText(row.get('evidence', String.class)) : []
        canonical in columnEvidence
    }

    static URI makeCitationURI(type, ref) {
        if (!ref) return null

        switch(type) {
            case 'PUBMED':
                return new URI("http://www.ncbi.nlm.nih.gov/pubmed/$ref")
            case 'ONLINE_RESOURCE':
                return new URI(ref)
            default:
                return null
        }
    }
}
