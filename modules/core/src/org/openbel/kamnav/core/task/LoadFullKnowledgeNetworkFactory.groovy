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

    final Expando cyr
    final CyProperty<Properties> cyProp

    private static final Logger log = LoggerFactory.getLogger(getClass())

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator() {
        log.info("Create new LoadFullKnowledgeNetwork task.")

        new TaskIterator(
            new CreateCyNetwork(cyr.cyApplicationManager, cyr.cyNetworkFactory,
                                cyr.cyNetworkViewFactory, cyr.cyNetworkManager,
                                cyr.cyNetworkViewManager, cyr.wsAPI),
            new LoadFullKnowledgeNetwork(cyr.cyApplicationManager, cyr.wsAPI),
            new ApplyPreferredStyleToCurrent(cyr.cyApplicationManager, cyr.cyEventHelper, cyr.visualMappingManager),
            new ApplyPreferredLayoutToCurrent(cyr.cyApplicationManager, cyr.cyLayoutAlgorithmManager, cyProp.properties))
    }
}
