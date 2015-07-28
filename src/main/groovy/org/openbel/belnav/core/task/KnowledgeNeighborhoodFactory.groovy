package org.openbel.belnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.task.AbstractNodeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.Task
import org.cytoscape.work.TaskIterator
import org.openbel.belnav.common.model.Edge
import org.openbel.belnav.ui.SearchNeighborhoodUI
import org.openbel.ws.api.WsAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static java.lang.Boolean.TRUE
import static org.cytoscape.model.CyTableUtil.getNodesInState
import static org.openbel.belnav.common.util.EdgeUtil.findEdge
import static org.openbel.belnav.common.util.EdgeUtil.makeEdge
import static org.openbel.belnav.common.util.NodeUtil.*
import static org.openbel.belnav.core.Util.addEvidenceForEdge
import static org.openbel.ws.api.BelUtil.isCausal

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
        def wsAPI = cyr.wsManager.get(cyr.wsManager.default)

        def selectedNodes = getNodesInState(cyNv.model, 'selected', true).
            findAll { node ->
                def row = cyNv.model.getRow(node)
                row.isSet('linked') && row.get('linked', Boolean.class) == TRUE
            }

        searchUI.neighborhoodFacet(
            new EvIterator(cyNv.model, selectedNodes, wsAPI),
            { item ->
                def param = ~/[A-Z]+:"?([^"),]+)"?/
                def entities = []
                def m = (item.edge.toString() =~ param)
                while (m.find()) entities << m.group(1)
                def functions = [item.edge.source.fx, item.edge.target.fx]
                def direction = (item.selectedNode.label == item.edge.source.label) ? 'Downstream' : 'Upstream'
                def description = [
                    direction: direction,
                    entities: entities,
                    functions: functions,
                    relationship: item.edge.relationship,
                    causal: isCausal(item.edge),
                    citation: item.citationName,
                    display: [
                        edge: item.edge.toString()
                    ]
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
                    def rel = edge.relationship

                    def cySource = findNode(cyN, s.label) ?: makeNode(cyN, s.id, s.fx.displayValue, s.label)
                    def cyTarget = findNode(cyN, t.label) ?: makeNode(cyN, t.id, t.fx.displayValue, t.label)
                    def cyE = findEdge(cyN, s.label, rel, t.label) ?: makeEdge(cyN, cySource, cyTarget, edge.id, rel)
                    addEvidenceForEdge(cyr.cyTableManager, cyr.cyTableFactory, wsAPI, cyN, cyE)
                }
                msg.info("Added ${edges.size()} edges in knowledge neighborhood.")

                cyr.cyEventHelper.flushPayloadEvents()
                cyr.visualMappingManager.getCurrentVisualStyle().apply(cyNv)
                cyNv.updateView()
            }
        )

        return new TaskIterator({
            run: {}
            cancel: {}
        } as Task)
    }

    private class EvIterator implements Iterator<Map<String, Object>> {

        private final CyNetwork cyN
        private final Map edgeNode
        private final WsAPI wsAPI
        private List<Edge> edges
        private Iterator evidenceIterator
        private int index

        public EvIterator(CyNetwork cyN, List<CyNode> nodes, WsAPI wsAPI) {
            this.cyN = cyN
            this.wsAPI = wsAPI
            this.edgeNode = [:]

            this.edges = nodes.collect { selectedCyNode ->
                def node = toNode(cyN, selectedCyNode)
                def edges = wsAPI.adjacentEdges(node)
                edges.each {
                    edgeNode[it.id] = node
                }
                edges
            }.flatten().unique()
            this.evidenceIterator = null
            this.index = 0
        }

        @Override
        boolean hasNext() {
            if (!evidenceIterator || !evidenceIterator.hasNext()) {
                if (index == (edges.size())) return false
                def edge = edges[index]
                evidenceIterator = wsAPI.getSupportingEvidence(edge).collect {
                    it.selectedNode = edgeNode[edge.id]
                    it.edge = edge
                    it
                }.iterator()

                index++
            }

            return evidenceIterator.hasNext()
        }

        @Override
        Map<String, Object> next() {
            if (!evidenceIterator || !evidenceIterator.hasNext()) {
                def edge = edges[index]
                evidenceIterator = wsAPI.getSupportingEvidence(edge).collect {
                    it.selectedNode = edgeNode[edge.id]
                    it.edge = edge
                    it
                }.iterator()

                index++
            }

            evidenceIterator.next()
        }

        @Override
        void remove() {
            throw new UnsupportedOperationException()
        }
    }
}
