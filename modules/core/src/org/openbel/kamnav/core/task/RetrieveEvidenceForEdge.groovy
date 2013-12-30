package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.ws.api.WsAPI

import static org.openbel.kamnav.core.Util.addEvidenceForEdge

@TupleConstructor
class RetrieveEvidenceForEdge extends AbstractTask {

    final CyNetworkView cyNv
    final CyEdge cyE
    final WsAPI wsAPI
    final CyTableFactory fac
    final CyTableManager mgr

    @Override
    void run(TaskMonitor m) throws Exception {
        m.title = "Retrieving evidence for edge."
        addEvidenceForEdge(mgr, fac, wsAPI, cyNv.model, cyE)
    }
}
