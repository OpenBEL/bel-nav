package org.openbel.kamnav.core.event

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.events.RowsSetEvent
import org.cytoscape.model.events.RowsSetListener
import org.openbel.kamnav.ui.EdgeUpdateable

@TupleConstructor
class ShowEdgeDetailListener implements RowsSetListener{

    final Expando cyr
    final EdgeUpdateable updateable

    @Override
    void handleEvent(RowsSetEvent evt) {
        // check for new selection
        def selected = evt.getColumnRecords(CyNetwork.SELECTED).find { it ->
            it.value == Boolean.TRUE
        }
        if (!selected) return;

        def suid = selected.row.get(CyNetwork.SUID, Long.class)
        if (!suid) return;

        def evTbl = cyr.cyTableManager.getAllTables(true).find { it.title == 'BEL.Evidence' }
        def evidence = evTbl.getMatchingRows('edge', suid)

        if (evidence) {
            def first = evidence.first()
            Long networkSUID = first.get('network', Long.class)
            if (!networkSUID) return;
            def cyN = cyr.cyNetworkManager.getNetwork(networkSUID)
            if (!cyN) return;

            updateable.update(evidence.collect { row ->
                [
                    statement: row.get('statement', String.class),
                    citation: [
                        name: row.get('citation name', String.class),
                        type: row.get('citation type', String.class),
                        id: row.get('citation id', String.class)
                    ],
                    annotations: row.allValues.findAll { k, v ->
                        k.startsWith('annotation: ')
                    }.collectEntries { k, v ->
                        [k.replaceFirst('annotation: ', ''), v]
                    }
                ]
            })
        } else {
            updateable.update([])
        }
    }
}
