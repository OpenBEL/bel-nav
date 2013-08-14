package org.openbel.kamnav.core.task

import org.cytoscape.application.CyApplicationManager
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import org.openbel.ws.api.WsAPI

class LinkKnowledgeNetwork extends AbstractTask {

    private final CyApplicationManager appMgr
    private final WsAPI wsAPI
    private final CyNetworkView cyNv

    // tunable state
    private String knName

    private LinkKnowledgeNetwork(final CyApplicationManager appMgr,
            final WsAPI wsAPI, final CyNetworkView cyNv) {
        this.appMgr = appMgr
        this.wsAPI = wsAPI
        this.cyNv = cyNv
    }

    @Tunable(description = "Knowledge network")
    public ListSingleSelection<String> getKnName() {
        String[] names = wsAPI.knowledgeNetworks().keySet() as String[]
        return new ListSingleSelection<String>(names)
    }

    public void setKnName(ListSingleSelection<String> lsel) {
        this.knName = lsel.selectedValue
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
    }
}
