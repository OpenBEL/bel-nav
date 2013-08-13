package org.openbel.kamnav.core.task

import org.cytoscape.application.CyApplicationManager
import org.cytoscape.task.AbstractNetworkViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator
import org.openbel.ws.api.WsAPI

class LinkKnowledgeNetworkFactory extends AbstractNetworkViewTaskFactory {

    private final CyApplicationManager appMgr
    private final WsAPI wsAPI

    LinkKnowledgeNetworkFactory(CyApplicationManager appMgr, WsAPI wsAPI) {
        this.appMgr = appMgr
        this.wsAPI = wsAPI
    }

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(CyNetworkView cyNv) {
        return new TaskIterator(new LinkKnowledgeNetwork(appMgr, wsAPI, cyNv))
    }
}
