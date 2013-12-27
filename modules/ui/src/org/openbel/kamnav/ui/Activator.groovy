package org.openbel.kamnav.ui

import groovy.swing.SwingBuilder
import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.service.util.AbstractCyActivator
import org.jdesktop.swingx.JXHyperlink
import org.jdesktop.swingx.JXList
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.JXTaskPane
import org.jdesktop.swingx.JXTaskPaneContainer
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        def swing = new SwingBuilder()
        swing.registerBeanFactory('taskPaneContainer', JXTaskPaneContainer.class)
        swing.registerBeanFactory('taskPane', JXTaskPane.class)
        swing.registerBeanFactory('jxList', JXList.class)
        swing.registerBeanFactory('jxTable', JXTable.class)
        swing.registerBeanFactory('jxHyperlink', JXHyperlink.class)

        EvidencePanel evPanel = new EvidencePanel(swing)
        registerAllServices(bc, evPanel, [name: 'evidence'] as Properties)

        SearchNodesDialogUI addDialog = new SearchNodesDialogUIImpl(swing)
        registerService(bc, addDialog, SearchNodesDialogUI.class, [:] as Properties)

        SearchNeighborhoodUI knDialog = new SearchNeighborhoodUIImpl(swing)
        registerService(bc, knDialog, SearchNeighborhoodUI.class, [:] as Properties)
    }
}
