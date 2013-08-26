package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.property.CyProperty
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.AbstractTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.ws.api.WsAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@TupleConstructor
class LoadFullKnowledgeNetworkFactory extends AbstractTaskFactory {

    private static final Logger log = LoggerFactory.getLogger(getClass())
    final CyApplicationManager appMgr
    final CyNetworkFactory cynFac
    final CyNetworkViewFactory cynvFac
    final CyNetworkManager cynMgr
    final CyNetworkViewManager cynvMgr
    final CyLayoutAlgorithmManager cylMgr
    final CyProperty<Properties> cyProp
    final CyEventHelper evtHelper
    final VisualMappingManager visMgr
    final WsAPI wsAPI

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator() {
        log.info("Create new LoadFullKnowledgeNetwork task.")

        new TaskIterator(
            new CreateCyNetwork(appMgr, cynFac, cynvFac, cynMgr, cynvMgr, wsAPI),
            new LoadFullKnowledgeNetwork(appMgr, wsAPI),
            new ApplyPreferredStyleToCurrent(appMgr, evtHelper, visMgr),
            new ApplyPreferredLayoutToCurrent(appMgr, cylMgr, cyProp.properties))
    }
}
