package org.openbel.kamnav.core

import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.task.NetworkViewTaskFactory
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.swing.DialogTaskManager
import org.openbel.kamnav.core.task.LinkKnowledgeNetworkFactory
import org.openbel.kamnav.core.task.LoadFullKnowledgeNetworkFactory
import org.openbel.ws.api.WsAPI
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        CyApplicationManager appMgr = getService(bc, CyApplicationManager.class)
        CyNetworkFactory cynFac = getService(bc, CyNetworkFactory.class)
        CyNetworkManager cynMgr = getService(bc, CyNetworkManager.class)
        CyNetworkViewFactory cynvFac = getService(bc, CyNetworkViewFactory.class)
        CyNetworkViewManager cynvMgr = getService(bc, CyNetworkViewManager.class)
        WsAPI wsAPI = getService(bc, WsAPI.class)

        registerService(bc,
            new LinkKnowledgeNetworkFactory(appMgr, wsAPI),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Link to Knowledge Network"
            ].asType(Properties.class))
        registerService(bc,
                new LoadFullKnowledgeNetworkFactory(cynFac, cynvFac, cynMgr, cynvMgr, wsAPI),
                TaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Create Network from Knowledge Network"
        ].asType(Properties.class))
    }
}
