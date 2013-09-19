package org.openbel.kamnav.core

import org.cytoscape.session.events.SessionLoadedListener
import org.openbel.kamnav.core.event.SessionLoadListener
import org.openbel.kamnav.core.task.AddBelColumnsToCurrentFactoryImpl

import static org.openbel.kamnav.core.Util.*
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
        def cyr = cyReference(bc, this.&getService, [CyApplicationManager.class,
                    CyNetworkFactory.class, CyNetworkManager.class,
                    CyNetworkViewFactory.class, CyNetworkViewManager.class,
                    CyLayoutAlgorithmManager.class, VisualMappingManager.class,
                    CyEventHelper.class, ApplyPreferredLayoutTaskFactory.class,
                    WsAPI.class] as Class<?>[])
        CyProperty<Properties> cyProp = getService(bc,CyProperty.class,"(cyPropertyName=cytoscape3.props)");

        // register listeners
        LoadVizmapFileTaskFactory vf =  getService(bc,LoadVizmapFileTaskFactory.class)
        registerService(bc,
            new SessionLoadListener(cyr.visualMappingManager, vf),
            SessionLoadedListener.class, [:] as Properties)

        // register tasks
        registerService(bc,
            new AddBelColumnsToCurrentFactoryImpl(cyr),
            AddBelColumnsToCurrentFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Add Data Columns"
            ] as Properties)
        registerService(bc,
            new ExpandNodeFactory(cyr),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Expand Node"
            ] as Properties)
        registerService(bc,
            new LinkKnowledgeNetworkFactory(cyr),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Link to Knowledge Network"
            ] as Properties)
        registerService(bc,
            new LoadFullKnowledgeNetworkFactory(cyr, cyProp),
            TaskFactory.class, [
                preferredMenu: 'File.New.Network',
                menuGravity: 11.0,
                title: 'From Knowledge Network'
            ] as Properties)

        // initialization
        contributeVisualStyles(cyr.visualMappingManager, vf)
    }
}
