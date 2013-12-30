package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.kamnav.core.Util
import org.openbel.ws.api.WsAPI

import static org.openbel.kamnav.core.Util.clearEvidenceTable
import static Boolean.TRUE

@TupleConstructor
class RetrieveEvidenceForNetwork extends AbstractTask {

    final CyNetworkView cyNv
    final WsAPI wsAPI
    final CyTableFactory fac
    final CyTableManager mgr

    @Override
    void run(TaskMonitor m) throws Exception {
        def cyN = cyNv.model

        def edges = cyN.edgeList.findAll { edge ->
            def row = cyNv.model.getRow(edge)
            row.isSet('linked') && row.get('linked', Boolean.class) == TRUE
        }
        m.title = "Retrieving evidence for ${edges.size()} edges."

        clearEvidenceTable(mgr, fac, cyN)
        def evForEdge = Util.&addEvidenceForEdge.curry(mgr, fac, wsAPI, cyN)
        edges.each(evForEdge)
    }
}
