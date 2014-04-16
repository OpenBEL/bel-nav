package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.task.AbstractNetworkViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator

@TupleConstructor
class LinkKnowledgeNetworkFactory extends AbstractNetworkViewTaskFactory {

    final Expando cyr

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(CyNetworkView cyNv) {
        def wsAPI = cyr.wsManager.get(cyr.wsManager.default)

        return new TaskIterator(
            new LinkKnowledgeNetwork(cyr.cyApplicationManager, wsAPI, cyNv),
            new RetrieveEvidenceForNetwork(cyNv, wsAPI, cyr.cyTableFactory, cyr.cyTableManager),
            new ApplyPreferredStyleToCurrent(cyr.cyApplicationManager, cyr.cyEventHelper, cyr.visualMappingManager)
        )
    }
}
