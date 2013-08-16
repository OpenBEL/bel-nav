package org.openbel.kamnav.core

import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.task.NetworkViewTaskFactory
import org.cytoscape.task.NodeViewTaskFactory
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.TaskFactory
import org.openbel.kamnav.core.task.ExpandNodeFactory
import org.openbel.kamnav.core.task.LinkKnowledgeNetworkFactory
import org.openbel.kamnav.core.task.LoadFullKnowledgeNetworkFactory
import org.openbel.ws.api.WsAPI
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    private static final String STYLE_PATH = '/style.props'
    private static final String[] STYLE_NAMES =
        ['KAM Association', 'KAM Visualization', 'KAM Visualization Minimal']

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

        // register tasks
        registerService(bc,
            new ExpandNodeFactory(wsAPI),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Expand Node"
            ].asType(Properties.class))
        registerService(bc,
            new LinkKnowledgeNetworkFactory(appMgr, wsAPI),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Link to Knowledge Network"
            ].asType(Properties.class))
        registerService(bc,
                new LoadFullKnowledgeNetworkFactory(appMgr, cynFac, cynvFac, cynMgr, cynvMgr, wsAPI),
                TaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Create Network from Knowledge Network"
        ].asType(Properties.class))

        // delete/add knowledge network styles (idempotent)
        VisualMappingManager vm = getService(bc,VisualMappingManager.class);
        LoadVizmapFileTaskFactory vf =  getService(bc,LoadVizmapFileTaskFactory.class)
        vm.allVisualStyles.findAll { it.title in STYLE_NAMES }.each(vm.&removeVisualStyle)
        vf.loadStyles(getClass().getResourceAsStream(STYLE_PATH))
    }
}
