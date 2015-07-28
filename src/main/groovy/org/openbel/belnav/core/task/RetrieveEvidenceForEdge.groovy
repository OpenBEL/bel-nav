package org.openbel.belnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskMonitor
import org.openbel.ws.api.WsAPI

import static org.openbel.belnav.core.Util.addEvidenceForEdge

@TupleConstructor
class RetrieveEvidenceForEdge extends BaseTask {

    final CyNetworkView cyNv
    final CyEdge cyE
    final WsAPI wsAPI
    final CyTableFactory fac
    final CyTableManager mgr

    @Override
    void doRun(TaskMonitor m) throws Exception {
        m.title = "Retrieving evidence for edge."
        addEvidenceForEdge(mgr, fac, wsAPI, cyNv.model, cyE)
    }
}
