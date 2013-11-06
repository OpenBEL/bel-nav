package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.task.AbstractNodeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.Task
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.ui.SearchNeighborhoodUI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.cytoscape.model.CyTableUtil.getNodesInState
import static org.openbel.kamnav.common.util.NodeUtil.toNode

@TupleConstructor
class KnowledgeNeighborhoodFactory extends AbstractNodeViewTaskFactory {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages");
    final Expando cyr
    final SearchNeighborhoodUI searchUI

    @Override
    boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
        def node = toNode(networkView.model, nodeView.model)
        if (!node.id) {
            msg.warn("${node.label} is not linked to a Knowledge Network.")
        }
        node.id
    }

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView cyNv) {
        def evidence = getNodesInState(cyNv.model, 'selected', true).collect {
            cyr.wsAPI.adjacentEdges(toNode(cyNv.model, it))
        }.flatten().unique().collect {
            cyr.wsAPI.getSupportingEvidence(it)
        }.flatten().unique().iterator()

        searchUI.neighborhoodFacet(cyr.cyApplicationManager, evidence, { item ->
            def description = [
                citation: item.citation,
            ].withDefault {[]}

            // dynamic annotation fields
            item.annotations.each { k, v ->
                description[k] << v
            }
            description
        }, {})

        return new TaskIterator({
            run: {}
            cancel: {}
        } as Task)
    }
}
