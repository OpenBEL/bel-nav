package org.openbel.belnav.ui

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel
import ca.odell.glazedlists.swing.DefaultEventTableModel
import groovy.swing.SwingBuilder
import groovy.transform.TupleConstructor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.awt.*

import static java.awt.GridBagConstraints.HORIZONTAL
import static java.awt.GridBagConstraints.LINE_START

@TupleConstructor
class SearchNodesDialogUIImpl implements SearchNodesDialogUI {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages");

    SwingBuilder swing

    /**
     * {@inheritDoc}
     */
    @Override
    void show(cySwing, preloadClosure, searchClosure, addClosure, options) {

        def mergedOptions = [
            knowledgeNetwork: 'Full - Human',
            function: 'All',
            connectToNetwork: false
        ] + options

        def JComboBox knBox, fxBox, nsBox
        def JTextField addlEntityField
        def JFileChooser fileChooser = new JFileChooser(
                dialogTitle: 'Choose a file with entities (one per line)',
                fileSelectionMode: JFileChooser.FILES_ONLY,
                multiSelectionEnabled: false)

        def kn = new BasicEventList()
        def fx = new BasicEventList()
        def ns = new BasicEventList()
        def entities = new BasicEventList()
        def searchItems = new BasicEventList()
        def filteredResults = new DefaultEventTableModel(searchItems,
            [
                getColumnCount: {3},
                getColumnName: {i -> ['Function', 'Label', 'Present'][i]},
                getColumnValue: {o, i ->
                    o."${['fx', 'label', 'present'][i]}"
                }
            ] as TableFormat
        )
        def itemsTable = swing.jxTable(model: filteredResults, columnControlVisible: true)
        itemsTable.columnModel.getColumn(0).minWidth = 160
        itemsTable.columnModel.getColumn(0).maxWidth = 160
        itemsTable.columnModel.getColumn(0).preferredWidth = 160
        itemsTable.columnModel.getColumn(0).resizable = false

        itemsTable.columnModel.getColumn(1).minWidth = 100
        itemsTable.columnModel.getColumn(1).resizable = true

        itemsTable.columnModel.getColumn(2).minWidth = 80
        itemsTable.columnModel.getColumn(2).maxWidth = 80
        itemsTable.columnModel.getColumn(2).preferredWidth = 80
        itemsTable.columnModel.getColumn(2).resizable = false
        itemsTable.rowSorter.sortKeys = [new RowSorter.SortKey(0, SortOrder.ASCENDING)]
        Font smallFont = new Font(UIManager.getFont("Label.font").name, Font.BOLD, 10)

        def dialog = swing.dialog(id: 'the_dialog', title: 'Search Nodes',
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, modal: false) {

            borderLayout()
            panel(border: titledBorder(title: 'Search Options'),
                    constraints: BorderLayout.NORTH) {

                borderLayout()
                panel(constraints: BorderLayout.CENTER) {
                    gridBagLayout()

                    label(text: 'Knowledge Network', constraints: gbc(anchor: LINE_START,
                            gridx: 0, gridy: 0,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.0, weighty: 0.1,
                            insets: [10, 2, 0, 0]))
                    knBox = comboBox(model: new DefaultEventComboBoxModel(kn),
                            constraints: gbc(anchor: LINE_START,
                            gridx: 1, gridy: 0,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.8, weighty: 0.1,
                            fill: HORIZONTAL, insets: [10, 0, 0, 0]))
                    label(text: 'Function', constraints: gbc(anchor: LINE_START,
                            gridx: 0, gridy: 1,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.0, weighty: 0.1,
                            insets: [10, 2, 0, 0]))
                    fxBox = comboBox(model: new DefaultEventComboBoxModel(fx),
                            constraints: gbc(anchor: LINE_START,
                            gridx: 1, gridy: 1,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.8, weighty: 0.1,
                            fill: HORIZONTAL, insets: [10, 0, 0, 0]))
                    label(text: 'Namespace', constraints: gbc(anchor: LINE_START,
                            gridx: 0, gridy: 2,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.0, weighty: 0.1,
                            insets: [10, 2, 0, 0]))
                    nsBox = comboBox(model: new DefaultEventComboBoxModel(ns),
                            constraints: gbc(anchor: LINE_START,
                            gridx: 1, gridy: 2,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.8, weighty: 0.1,
                            fill: HORIZONTAL, insets: [10, 0, 0, 0]))
                    label(text: "Entities\n(use * for wildcard)", constraints: gbc(anchor: LINE_START,
                            gridx: 0, gridy: 3,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.0, weighty: 0.1,
                            insets: [10, 2, 0, 0]))
                    addlEntityField = textField(constraints: gbc(anchor: LINE_START,
                            gridx: 1, gridy: 3,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.8, weighty: 0.1,
                            fill: HORIZONTAL, insets: [10, 0, 0, 0]))
                    button(text: 'Load entities from file', actionPerformed: {
                        doOutside {
                            int ret = fileChooser.showOpenDialog(the_dialog)
                            if (ret == JFileChooser.APPROVE_OPTION) {
                                def file = fileChooser.selectedFile
                                def lines = []
                                file.eachLine('utf-8', {
                                    if (!it?.trim()) return
                                    lines.add(it.trim())
                                })
                                lines = lines.unique().sort()
                                edt {
                                    entities.removeAll {true}
                                    entities.addAll(lines)
                                    fileMessage.text = "${lines.size()} unique entities"
                                }
                                searchButton.enabled = true
                            }
                        }
                    }, constraints: gbc(anchor: LINE_START, gridx: 0, gridy: 4,
                            gridwidth: 1, gridheight: 1, weightx: 0.2, weighty: 0.1,
                            insets: [10, 0, 0, 0]))
                    label(id: 'fileMessage', constraints: gbc(anchor: LINE_START,
                            gridx: 1, gridy: 4, gridwidth: 1, gridheight: 1,
                            weightx: 0.0, weighty: 0.1, fill: HORIZONTAL,
                            insets: [10, 2, 0, 0]))
                }
            }

            panel(constraints: BorderLayout.CENTER) {
                borderLayout()
                scrollPane(constraints: BorderLayout.CENTER, viewportView: itemsTable)
                panel(constraints: BorderLayout.SOUTH) {
                    gridLayout(rows: 2, columns: 1)
                    panel() {
                        borderLayout()
                        checkBox(id: 'connectChk', text: 'Connect to network',
                                 selected: mergedOptions.connectToNetwork,
                                 constraints: BorderLayout.WEST)
                        label(text: 'Search table with Control + F (table must be selected).', font: smallFont,
                              constraints: BorderLayout.EAST)
                    }
                    panel() {
                        flowLayout(alignment: FlowLayout.RIGHT)
                        label(id: 'busyIcon', icon: imageIcon(resource: '/busy.gif'), visible: false)
                        label(id: 'resultsMessage')
                    }
                }
            }

            panel(constraints: BorderLayout.SOUTH) {
                flowLayout(alignment: FlowLayout.RIGHT)
                button(id: 'cancelButton', preferredSize: [85, 25],
                       action: action(name: 'Cancel', mnemonic: 'C', closure: {
                           the_dialog.dispose()
                       }))
                button(id: 'searchButton', preferredSize: [85, 25],
                       action: action(name: 'Search', mnemonic: 'S', closure: {
                           busyIcon.visible = true
                           doOutside {
                               def allEntities = entities + addlEntityField.text.split(',').collect{it.trim()}.findAll()
                               def (nodes, message) = searchClosure.call(
                                   knBox.selectedItem,
                                   fxBox.selectedItem == 'All' ? null : fxBox.selectedItem,
                                   nsBox.selectedItem,
                                   allEntities
                               )
                               edt {
                                   searchItems.removeAll {true}
                                   searchItems.addAll(nodes)
                                   resultsMessage.text = message
                                   busyIcon.visible = false
                               }
                           }
                       }))
                button(id: 'addButton', preferredSize: [85, 25],
                       action: action(name: 'Add', mnemonic: 'A', closure: {
                           busyIcon.visible = true
                           doOutside {
                               def selected = itemsTable.selectedRows.
                                       collect(itemsTable.&convertRowIndexToModel).
                                       collect {searchItems.get(it)}
                               def added = addClosure.call(selected, connectChk.selected)

                               swing.edt {
                                   if (added) {
                                       def items = new ArrayList(searchItems)
                                       searchItems.removeAll {true}
                                       searchItems.addAll(items)
                                       itemsTable.invalidate()
                                       itemsTable.doLayout()
                                   }
                                   busyIcon.visible = false
                               }
                           }
                       }))
            }
        }

        // fetch preload
        swing.doOutside {
            Map data = preloadClosure.call()
            swing.edt {
                kn.addAll(data.knowledgeNetworks)
                knBox.selectedItem = mergedOptions.knowledgeNetwork

                ns.add('All')
                ns.addAll(data.namespaces)
                nsBox.selectedItem = 'All'

                fx.addAll(['All'] + data.functions)
                fxBox.selectedItem = mergedOptions.function
            }
        }

        dialog.pack()
        dialog.size = [700, 700]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }
}
