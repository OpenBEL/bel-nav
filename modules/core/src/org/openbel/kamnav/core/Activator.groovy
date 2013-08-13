package org.openbel.kamnav.core

import org.cytoscape.application.CyApplicationManager
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.task.NetworkViewTaskFactory
import org.openbel.kamnav.core.task.LinkKnowledgeNetworkFactory
import org.openbel.ws.api.WsAPI
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        CyApplicationManager appMgr = getService(bc, CyApplicationManager.class)
        WsAPI wsAPI = getService(bc, WsAPI.class)

        registerService(bc,
            new LinkKnowledgeNetworkFactory(appMgr, wsAPI),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Link to Knowledge Network"
            ].asType(Properties.class))
    }
}
