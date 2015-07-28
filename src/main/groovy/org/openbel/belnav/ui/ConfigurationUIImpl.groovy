package org.openbel.belnav.ui

import groovy.swing.SwingBuilder
import org.openbel.ws.api.WsManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JTable
import javax.swing.JTextField
import java.awt.BorderLayout
import java.awt.FlowLayout

import static java.awt.GridBagConstraints.*

class ConfigurationUIImpl implements ConfigurationUI {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages");

    @Override
    JDialog configurationDialog(WsManager wsManager, Closure auth, Closure save) {

        def swing = new SwingBuilder()
        def JTable configurationsTable
        def dialog = swing.dialog(id: 'the_dialog', title: 'Configure OpenBEL',
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {

            def dialog = the_dialog
            def JTextField url
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {

                borderLayout()
                panel(constraints: BorderLayout.CENTER) {
                    gridBagLayout()

                    label(text: 'URL', constraints: gbc(anchor: LINE_START,
                            gridx: 0, gridy: 0,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.0, weighty: 0.1,
                            fill: HORIZONTAL, insets: [10, 2, 0, 0]))
                    url = textField(constraints: gbc(anchor: LINE_START,
                            gridx: 1, gridy: 0,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.2, weighty: 0.1,
                            fill: HORIZONTAL, insets: [10, 0, 0, 0]))
                    button(text: 'Add', preferredSize: [85, 25],
                            constraints: gbc(anchor: LAST_LINE_END,
                            gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                            weightx: 0.3, weighty: 0.1, insets: [0, 0, 0, 0]),
                            actionPerformed: {
                                doOutside {
                                    def urlVal = url.text.toLowerCase()
                                    def response = auth.call(urlVal)
                                    if (response) {
                                        // add to table
                                        def item = new Expando(
                                                url: new URI(urlVal),
                                                default: false,
                                                toString: { new URI(urlVal).toString() })
                                        resTable.model.with {
                                            rowsModel.value.add(item)
                                            fireTableDataChanged()
                                        }
                                    } else {
                                        edt {
                                            MessagePopups.errorConnectionAccess(urlVal)
                                        }
                                    }
                                }
                    })
                }
            }

            panel(constraints: BorderLayout.CENTER) {

                borderLayout()
                scrollPane(constraints: BorderLayout.CENTER) {

                    configurationsTable = table(id: 'resTable') {
                        tableModel {
                            closureColumn(header:  'Default', type: Boolean.class,
                                    read: {r -> r.default},
                                    write: {r, v ->
                                        if (v) {
                                            resTable.model.with {
                                                rowsModel.value.each {
                                                    it.default = false
                                                }
                                                fireTableDataChanged()
                                            }
                                        }
                                        r.default = v
                                    })
                            closureColumn(header: 'URL', read: {it.toString()})
                        }
                    }

                    // adjust column widths
                    configurationsTable.columnModel.getColumn(0).minWidth = 60
                    configurationsTable.columnModel.getColumn(0).maxWidth = 60
                    configurationsTable.columnModel.getColumn(0).preferredWidth = 60
                    configurationsTable.columnModel.getColumn(0).resizable = false
                    configurationsTable.columnModel.getColumn(1).minWidth = 100
                    configurationsTable.columnModel.getColumn(1).resizable = true
                }
                panel(constraints: BorderLayout.EAST) {

                    button(icon: Util.icon('/delete.png', 'Delete selection'), actionPerformed: {
                        def data = configurationsTable.model.rowsModel.value
                        configurationsTable.selectedRows.
                                collect(configurationsTable.&convertRowIndexToModel).
                                collect(data.&get).
                                each(data.&remove)
                        configurationsTable.model.with {
                            fireTableDataChanged()
                        }
                    })
                }
            }

            panel(constraints: BorderLayout.SOUTH) {
                flowLayout(alignment: FlowLayout.RIGHT)
                button(text: 'Cancel', preferredSize: [85, 25],
                        actionPerformed: {dialog.dispose()})
                button(text: 'OK', preferredSize: [85, 25], actionPerformed: {
                    swing.doOutside {
                        configurationsTable.model.with {
                            def rows = rowsModel.value
                            save.call(rows as Set)
                        }

                        the_dialog.dispose()
                        msg.info('BEL Navigator configuration saved successfully.')
                    }
                })
            }
        }

        // load table with existing configuration from WsManager
        swing.edt {
            configurationsTable.model.with {
                wsManager.all.collect { serviceLocation ->
                    new Expando(
                            url: serviceLocation,
                            default: wsManager.default == serviceLocation,
                            toString: { serviceLocation })
                }.each(rowsModel.value.&add)
                fireTableDataChanged()
            }
        }

        dialog.pack()
        dialog.size = [700, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }
}
