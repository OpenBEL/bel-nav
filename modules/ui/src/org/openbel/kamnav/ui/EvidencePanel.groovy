package org.openbel.kamnav.ui

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.DefaultEventTableModel
import groovy.swing.SwingBuilder
import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.application.swing.CytoPanelName
import org.jdesktop.swingx.JXHyperlink
import org.jdesktop.swingx.JXTable

import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import java.awt.Component
import static CytoPanelName.EAST
import static java.awt.GridBagConstraints.*
import static javax.swing.ListSelectionModel.SINGLE_SELECTION

class EvidencePanel implements CytoPanelComponent, Updateable {

    final SwingBuilder swing
    final JPanel panel

    JLabel citationName
    JXHyperlink citationLink
    JXTable stmtTable
    List statements
    List annotations

    EvidencePanel(SwingBuilder swing) {
        this.swing = swing
        statements = new BasicEventList()
        annotations = new BasicEventList()
        def stmtModel = new DefaultEventTableModel(statements,
            [
                getColumnCount: {1},
                getColumnName: {'Statement'},
                getColumnValue: {o, i -> o}
            ] as TableFormat)
        def annoModel = new DefaultEventTableModel(annotations,
            [
                getColumnCount: {2},
                getColumnName: {i -> ['Type', 'Value'][i]},
                getColumnValue: {o, i ->
                    switch(i) {
                        case 0: return o.type
                        case 1: return o.value
                    }
                }
            ] as TableFormat
        )
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
                                    def stmt = selection.first()
                                    def type = stmt.ev.citation.type
                                    def id = stmt.ev.citation.id
                                    def name = stmt.ev.citation.name
                                    citationName.text = name
                                    citationLink.text = id ?: ''
                                    citationLink.URI = makeCitationURI(type, id)

                                    annotations.removeAll {true}
                                    annotations.addAll(stmt.ev.annotations.collect { k, v ->
                                        new Expando(type: k, value: v)
                                    }.findAll {it.value}.sort {it.type})
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
                jxTable(model: annoModel, columnControlVisible: false)
            }
        }
    }

    def update(evidence) {
        swing.edt {
            // clear citation
            citationName.text = ''
            citationLink.text = ''

            // clear annotations
            annotations.removeAll {true}

            // update statement table
            statements.removeAll {true}
            statements.addAll(evidence.collect {
                new Expando(stmt: it.statement, ev: it, toString: {stmt})
            }.sort {it.stmt})

            if (stmtTable.getRowCount())
                stmtTable.selectionModel.setSelectionInterval(0, 0)
        }
    }

    @Override
    Component getComponent() { panel }
    @Override
    CytoPanelName getCytoPanelName() { EAST }
    @Override
    String getTitle() { "Evidence" }
    @Override
    Icon getIcon() { null }

    static URI makeCitationURI(type, id) {
        if (!id) return null

        switch(type) {
            case 'PUBMED':
                return new URI("http://www.ncbi.nlm.nih.gov/pubmed/$id")
            case 'ONLINE_RESOURCE':
                return new URI(id)
            default:
                return null
        }
    }
}
