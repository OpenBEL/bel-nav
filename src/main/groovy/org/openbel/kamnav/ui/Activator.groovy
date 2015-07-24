package org.openbel.kamnav.ui

import groovy.swing.SwingBuilder
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.application.swing.CySwingApplication
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingManager
import org.jdesktop.swingx.*
import org.osgi.framework.BundleContext

import static org.openbel.kamnav.common.Constant.setLoggingExceptionHandler
import static org.openbel.kamnav.common.util.Util.cyReference

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        def cyr = cyReference(bc, this.&getService, [CyApplicationManager.class,
                CySwingApplication.class, CyNetworkFactory.class, CyNetworkManager.class,
                CyNetworkViewFactory.class, CyNetworkViewManager.class,
                CyLayoutAlgorithmManager.class, CyTableFactory.class, CyTableManager.class,
                VisualMappingManager.class, CyEventHelper.class] as Class<?>[])

        def swing = new SwingBuilder()
        swing.registerBeanFactory('taskPaneContainer', JXTaskPaneContainer.class)
        swing.registerBeanFactory('taskPane', JXTaskPane.class)
        swing.registerBeanFactory('jxList', JXList.class)
        swing.registerBeanFactory('jxTable', JXTable.class)
        swing.registerBeanFactory('jxHyperlink', JXHyperlink.class)

        ConfigurationUI configUI = new ConfigurationUIImpl()
        registerService(bc, configUI, ConfigurationUI.class, [:] as Properties)

        registerAllServices(bc, new EvidencePanelComponent(cyr,
                new EvidencePanel(swing, cyr)), [name: 'evidence'] as Properties)

        SearchNodesDialogUI addDialog = new SearchNodesDialogUIImpl(swing)
        registerService(bc, addDialog, SearchNodesDialogUI.class, [:] as Properties)

        SearchNeighborhoodUI knDialog = new SearchNeighborhoodUIImpl(swing)
        registerService(bc, knDialog, SearchNeighborhoodUI.class, [:] as Properties)

        setLoggingExceptionHandler()
    }
}
