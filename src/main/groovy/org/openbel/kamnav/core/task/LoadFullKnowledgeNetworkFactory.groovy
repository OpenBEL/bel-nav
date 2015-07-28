package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.property.CyProperty
import org.cytoscape.work.AbstractTaskFactory
import org.cytoscape.work.TaskIterator
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

        def wsAPI = cyr.wsManager.get(cyr.wsManager.default)
        new TaskIterator(
            new CreateCyNetwork(cyr.cyApplicationManager, cyr.cyNetworkFactory,
                                cyr.cyNetworkViewFactory, cyr.cyNetworkManager,
                                cyr.cyNetworkViewManager, wsAPI),
            new LoadFullKnowledgeNetwork(cyr.cyApplicationManager, wsAPI),
            new ApplyPreferredStyleToCurrent(cyr.cyApplicationManager, cyr.cyEventHelper, cyr.visualMappingManager),
            new ApplyPreferredLayoutToCurrent(cyr.cyApplicationManager, cyr.cyLayoutAlgorithmManager, cyProp.properties))
    }
}
