package org.openbel.kamnav.ui

import groovy.transform.TupleConstructor
import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.application.swing.CytoPanelName
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.events.RowsSetEvent
import org.cytoscape.model.events.RowsSetListener

import javax.swing.*
import java.awt.*

import static org.cytoscape.application.swing.CytoPanelName.EAST

@TupleConstructor
class EvidencePanelComponent implements CytoPanelComponent, EdgeUpdateable, RowsSetListener {

    final Expando cyr
    final EvidencePanel evPanel

    @Override
    Component getComponent() { evPanel.panel }
    @Override
    CytoPanelName getCytoPanelName() { EAST }
    @Override
    String getTitle() { "Evidence" }
    @Override
    Icon getIcon() { null }

    @Override
    void handleEvent(RowsSetEvent evt) {
        // check for new selection
        def selected = evt.getColumnRecords(CyNetwork.SELECTED).find {
            it.value == Boolean.TRUE
        }
        if (!selected) return

        def suid = selected.row.get(CyNetwork.SUID, Long.class)
        if (!suid) return

        def evTbl = cyr.cyTableManager.getAllTables(true).find { it.title == 'BEL.Evidence' }
        if (!evTbl) return

        def evidence = evTbl.getMatchingRows('edge', suid)
        if (evidence) {
            def first = evidence.first()
            Long networkSUID = first.get('network', Long.class)
            if (!networkSUID) return
            def cyN = cyr.cyNetworkManager.getNetwork(networkSUID)
            if (!cyN) return

            evPanel.update(evidence.collect { row ->
                [
                    network: networkSUID,
                    edge: suid,
                    bel_statement: row.get('statement', String.class),
                    citation: [
                            name: row.get('citation name', String.class),
                            type: row.get('citation type', String.class),
                            reference: row.get('citation reference', String.class)
                    ],
                    biological_context: row.allValues.findAll { k, v ->
                        k.startsWith('annotation: ')
                    }.collectEntries { k, v ->
                        [k.replaceFirst('annotation: ', ''), v]
                    }
                ]
            })
        } else {
            evPanel.update([])
        }
    }

    @Override
    def update(edge) {
        return evPanel.update(edge)
    }
}
