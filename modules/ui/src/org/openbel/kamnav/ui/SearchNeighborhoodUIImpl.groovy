package org.openbel.kamnav.ui

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.event.ListEventListener
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.DefaultEventTableModel
import groovy.swing.SwingBuilder
import groovy.transform.TupleConstructor
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.decorator.HighlightPredicate
import org.jdesktop.swingx.decorator.ToolTipHighlighter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.RowSorter.SortKey
import javax.swing.SortOrder
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout

import static javax.swing.ScrollPaneConstants.*
import static org.openbel.kamnav.common.facet.Functions.*

@TupleConstructor
class SearchNeighborhoodUIImpl implements SearchNeighborhoodUI {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")
    SwingBuilder swing

    @Override
    JDialog neighborhoodFacet(Iterator<Map> evidenceIterator,
                              Closure denormalize, Closure addEdges) {
        def items = [evidenceIterator.take(1).next()]
        def fieldDescriptions = []

        def columns = ['Edge', 'Statement', 'Causal', 'Species', 'Citation']

        fieldDescriptions.addAll(describe(items, denormalize))
        def facets = facet(fieldDescriptions) as Map

        def resultEventList = new BasicEventList()
        resultEventList.addAll(fieldDescriptions)
        def filteredResults = new DefaultEventTableModel(resultEventList,
                [
                        getColumnCount: {columns.size()},
                        getColumnName: {i -> columns[i]},
                        getColumnValue: {o, i ->
                            switch(i) {
                                case 0: return o.edge.toString()
                                case 1: return o.statement
                                case 2: return o.causal ? 'Yes' : 'No'
                                case 3:
                                    return o.species ? o.species.first() : 'N/A'
                                case 4: return o.citation
                            }
                        }
                ] as TableFormat
        )
        def resultsLabel
        resultEventList.addListEventListener({
            listChanged: {
                resultsLabel.text = "Total: ${resultEventList.size()}"
            }
        } as ListEventListener)
        def addPathsButton
        def results = swing.jxTable(model: filteredResults, columnControlVisible: true)
        results.selectionModel.addListSelectionListener([
            valueChanged: { evt ->
                addPathsButton.enabled = results.selectedRows
            }
        ] as ListSelectionListener)

        def facetFieldModels = []
        def createTaskPane = { String k, v ->
            def title = k.split('_').collect{it.capitalize()}.join(' ')
            def facetFieldModel = new FacetFieldModel(facets, facets[k], fieldDescriptions, resultEventList, results)
            def tbl
            def pane = swing.taskPane(title: title, animated: false, collapsed: true) {
                scrollPane(border: lineBorder(thickness: 1, color: Color.black),
                        constraints: BorderLayout.CENTER, background: Color.white) {
                    tbl = swing.table(model: facetFieldModel, autoCreateRowSorter: true)
                }
            }
            tbl.columnModel.getColumn(0).minWidth = 40
            tbl.columnModel.getColumn(0).maxWidth = 40
            tbl.columnModel.getColumn(0).preferredWidth = 40
            tbl.columnModel.getColumn(0).resizable = false

            tbl.columnModel.getColumn(1).minWidth = 40
            tbl.columnModel.getColumn(1).maxWidth = 40
            tbl.columnModel.getColumn(1).preferredWidth = 40
            tbl.columnModel.getColumn(1).resizable = false

            tbl.columnModel.getColumn(2).minWidth = 100
            tbl.columnModel.getColumn(2).resizable = true

            tbl.columnModel.getColumn(3).minWidth = 60
            tbl.columnModel.getColumn(3).maxWidth = 60
            tbl.columnModel.getColumn(3).preferredWidth = 60
            tbl.columnModel.getColumn(3).resizable = false
            tbl.rowSorter.sortKeys = [new SortKey(3, SortOrder.DESCENDING)]
            facetFieldModels << facetFieldModel
            pane
        }

        def dialog = swing.dialog(id: 'path_facet_dialog', title: 'Pathfind',
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, modal: false) {

            panel {
                borderLayout()
                splitPane(constraints: BorderLayout.CENTER, continuousLayout: true,
                          dividerLocation: 400, oneTouchExpandable: true,
                    leftComponent: scrollPane(id: 'facetScrollPane',
                            horizontalScrollBarPolicy: HORIZONTAL_SCROLLBAR_NEVER,
                            verticalScrollBarPolicy: VERTICAL_SCROLLBAR_AS_NEEDED,
                            minimumSize: [400, 600]) {
                        taskPaneContainer(id: 'facetContainer') {
                            facets.each(createTaskPane)
                        }
                    },
                    rightComponent: panel {
                        borderLayout()
                        scrollPane(id: 'resultScrollPane', viewportView: results,
                                constraints: BorderLayout.CENTER)
                        panel(constraints: BorderLayout.SOUTH) {
                            resultsLabel = label()
                        }
                    }
                )
                panel(constraints: BorderLayout.SOUTH) {
                    flowLayout(alignment: FlowLayout.RIGHT)
                    button(defaultButton: true, action: action(name: 'Cancel', mnemonic: 'C', closure: {
                        path_facet_dialog.dispose()
                    }))
                    addPathsButton = button(action: action(enabled: false, name: 'Add Selected Edges', mnemonic: 'A', closure: {
                        swing.doOutside {
                            def sel = results.selectedRows.
                                collect(results.&convertRowIndexToModel).
                                collect {resultEventList.get(it)}
                            def selItems = sel.
                                collect(fieldDescriptions.&indexOf).
                                collect(items.&get)
                            addEdges.call(selItems.collect{it.edge})
                        }
                    }))
                }
            }
        }
        results.addHighlighter(new ToolTipHighlighter(HighlightPredicate.IS_TEXT_TRUNCATED))

        dialog.pack()
        dialog.size = [1000, 600]
        dialog.locationRelativeTo = null
        dialog.visible = true

        swing.doOutside {
            while (evidenceIterator.hasNext()) {
                // load in 100 chunk increments
                def next = evidenceIterator.take(100).toList()
                items += next
                def descriptions = describe(next, denormalize)

                swing.edt {
                    fieldDescriptions.addAll(descriptions)
                    resultEventList.addAll(descriptions)

                    def newFacets = facet(descriptions)
                    def newFacetKeys = newFacets.keySet() - facets.keySet()
                    mergeFacets([facets, newFacets])
                    newFacets.each { k, v ->
                        if (k in newFacetKeys) {
                            def taskPane = createTaskPane.call(k, v)
                            facetContainer.add(taskPane)
                            facetScrollPane.doLayout()
                        }
                    }
                    facetFieldModels.each {it.fireTableDataChanged()}
                }
            }
        }

        dialog
    }

    @TupleConstructor
    private final class FacetFieldModel extends AbstractTableModel {

        Map facets
        Map facetField
        List allResults
        List filteredResults
        JXTable resultsTable

        @Override
        int getRowCount() {
            return facetField.size()
        }

        @Override
        int getColumnCount() {
            return 4
        }

        @Override
        String getColumnName(int col) {
            switch(col) {
                case 0: return 'Incl'
                case 1: return 'Excl'
                case 2: return 'Value'
                case 3: return '#'
            }
        }

        @Override
        Class<?> getColumnClass(int col) {
            switch(col) {
                case 0: return Boolean.class
                case 1: return Boolean.class
                case 2: return Object.class
                case 3: return Integer.class
            }
        }

        @Override
        boolean isCellEditable(int row, int col) {
            col == 0 || col == 1
        }

        @Override
        Object getValueAt(int row, int col) {
            def fieldValue = facetField.values().toList().get(row)
            switch(col) {
                case 0: return fieldValue.filterComparison == 'inclusion'
                case 1: return fieldValue.filterComparison == 'exclusion'
                case 2: return fieldValue.value
                case 3: return fieldValue.count
            }
        }

        @Override
        void setValueAt(Object val, int row, int col) {
            def fieldValue = facetField.values().toList().get(row)
            switch(col) {
                case 0:
                    fieldValue.filterComparison = (val ? 'inclusion' : 'unset'); break
                case 1:
                    fieldValue.filterComparison = (val ? 'exclusion' : 'unset'); break
                case 2: fieldValue.value = val; break
                case 3: fieldValue.count = val; break
            }

            swing.edt {
                def res = filter(allResults, allResults, facets)
                filteredResults.removeAll {true}
                filteredResults.addAll(res)
                resultsTable.doLayout()
            }
        }
    }

    private static def denormalizePath = { item ->
        def param = ~/[A-Z]+:"?([^"),]+)"?/
        def relationships = item.path.collect {it.relationship}.findAll().unique()
        def causal = [
                'increases', 'decreases', 'directlyIncreases',
                'directlyDecreases', 'rateLimitingStepOf'
        ]
        def nodes = item.path.collect {it.label}.findAll()
        def intermediate_terms = nodes.subList(1, nodes.size() - 1)
        def intermediate_entities = intermediate_terms.collect {
            def m = (it =~ param)
            def l = []
            while (m.find()) l << m.group(1)
            l
        }.findAll().flatten()

        def start_entities = []
        def m = (item.start.label =~ param)
        while (m.find()) start_entities << m.group(1)

        def end_entities = []
        m = (item.end.label =~ param)
        while (m.find()) end_entities << m.group(1)

        // static fields
        def description = [
                causal: relationships.every {it in causal},
                start_term: item.start.label,
                end_term: item.end.label,
                intermediate_terms: intermediate_terms,
                start_entity: start_entities,
                end_entity: end_entities,
                intermediate_entities: intermediate_entities,
                length: (int) (item.path.size() / 2),
                relationships: relationships
        ].withDefault {[]}

        // dynamic annotation fields
        item.path.collect { it.evidence*.annotations }.
                flatten().findAll().
                inject([:].withDefault { [] }) { agg, next ->
                    next.each { k, v ->
                        agg[k] << v
                    }
                    agg
                }.each { k, v ->
            v.unique().each {
                description[k] << it
            }
        }
        description
    }
}
