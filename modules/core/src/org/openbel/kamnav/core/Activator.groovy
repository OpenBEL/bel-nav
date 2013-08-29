package org.openbel.kamnav.core

import org.openbel.kamnav.core.task.AddBelColumnsToCurrentFactoryImpl

import static org.openbel.kamnav.core.Constant.*
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.property.CyProperty
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.task.NetworkViewTaskFactory
import org.cytoscape.task.NodeViewTaskFactory
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
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
        CyLayoutAlgorithmManager cylMgr = getService(bc, CyLayoutAlgorithmManager.class)
        VisualMappingManager visMgr = getService(bc, VisualMappingManager.class)
        CyEventHelper evtHelper = getService(bc, CyEventHelper.class)
        ApplyPreferredLayoutTaskFactory aplFac = getService(bc, ApplyPreferredLayoutTaskFactory.class)
        CyProperty<Properties> cyProp = getService(bc,CyProperty.class,"(cyPropertyName=cytoscape3.props)");
        WsAPI wsAPI = getService(bc, WsAPI.class)

        // register tasks
        registerService(bc,
            new AddBelColumnsToCurrentFactoryImpl(appMgr, evtHelper, visMgr),
            AddBelColumnsToCurrentFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Add Data Columns"
            ] as Properties)
        registerService(bc,
            new ExpandNodeFactory(aplFac, evtHelper, visMgr, wsAPI),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Expand Node"
            ] as Properties)
        registerService(bc,
            new LinkKnowledgeNetworkFactory(appMgr, wsAPI),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Link to Knowledge Network"
            ] as Properties)
        registerService(bc,
            new LoadFullKnowledgeNetworkFactory(appMgr, cynFac, cynvFac,
                                                cynMgr, cynvMgr, cylMgr,
                                                cyProp, evtHelper, visMgr,
                                                wsAPI),
            TaskFactory.class, [
                preferredMenu: 'File.New.Network',
                menuGravity: 11.0,
                title: 'From Knowledge Network'
            ] as Properties)

        // delete/add knowledge network styles (idempotent)
        LoadVizmapFileTaskFactory vf =  getService(bc,LoadVizmapFileTaskFactory.class)
        visMgr.allVisualStyles.findAll { it.title in STYLE_NAMES }.each(visMgr.&removeVisualStyle)
        vf.loadStyles(getClass().getResourceAsStream(STYLE_PATH))
    }
}
