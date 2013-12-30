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

import static java.lang.Boolean.TRUE
import static org.cytoscape.model.CyTableUtil.getNodesInState
import static org.openbel.kamnav.common.util.EdgeUtil.*
import static org.openbel.kamnav.common.util.NodeUtil.*
import static org.openbel.kamnav.core.Util.addEvidenceForEdge
import static org.openbel.ws.api.BelUtil.*

@TupleConstructor
class KnowledgeNeighborhoodFactory extends AbstractNodeViewTaskFactory {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages");
    final Expando cyr
    final SearchNeighborhoodUI searchUI

    @Override
    boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
        // at least one linked node needs to be selected; the unlinked nodes
        // will be filtered once the task executes.
        def selected = getNodesInState(networkView.model, 'selected', true)
        selected.find { node ->
            def row = networkView.model.getRow(node)
            row.isSet('linked') && row.get('linked', Boolean.class) == TRUE
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView cyNv) {
        def evidence = getNodesInState(cyNv.model, 'selected', true).
        findAll { node ->
            def row = cyNv.model.getRow(node)
            row.isSet('linked') && row.get('linked', Boolean.class) == TRUE
        }.collect {
            cyr.wsAPI.adjacentEdges(toNode(cyNv.model, it))
        }.flatten().unique().collect { edge ->
            def ev = cyr.wsAPI.getSupportingEvidence(edge)
            ev.each { stmt ->
                stmt.edge = edge
            }
            ev
        }.flatten().unique().iterator()

        searchUI.neighborhoodFacet(evidence.iterator(), { item ->
            def param = ~/[A-Z]+:"?([^"),]+)"?/
            def entities = []
            def m = (item.edge.toString() =~ param)
            while (m.find()) entities << m.group(1)
            def description = [
                edge: item.edge,
                entities: entities,
                statement: belStatement(item),
                causal: isCausal(item.edge),
                citation: item.citation
            ].withDefault {[]}

            // dynamic annotation fields
            item.annotations.each { k, v ->
                description[k.toLowerCase()] << v
            }
            description
        }, { edges ->
            edges.each { edge ->
                def cyN = cyr.cyApplicationManager.currentNetwork

                def s = edge.source
                def t = edge.target
                def rel = edge.relationship.displayValue

                def cySource = findNode(cyN, s.label) ?: makeNode(cyN, s.id, s.fx.displayValue, s.label)
                def cyTarget = findNode(cyN, t.label) ?: makeNode(cyN, t.id, t.fx.displayValue, t.label)
                def cyE = findEdge(cyN, s.label, rel, t.label) ?: makeEdge(cyN, cySource, cyTarget, edge.id, rel)
                addEvidenceForEdge(cyr.cyTableManager, cyr.cyTableFactory, cyr.wsAPI, cyN, cyE)
            }
            msg.info("Added ${edges.size()} edges in knowledge neighborhood.")

            cyr.cyEventHelper.flushPayloadEvents()
            cyr.visualMappingManager.getCurrentVisualStyle().apply(cyNv)
            cyNv.updateView()
        })

        return new TaskIterator({
            run: {}
            cancel: {}
        } as Task)
    }
}
