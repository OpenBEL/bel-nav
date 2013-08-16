package org.openbel.kamnav.core.task

import org.cytoscape.model.CyEdge
import org.openbel.ws.api.WsAPI

import static java.lang.String.format
import static org.openbel.framework.common.enums.FunctionEnum.fromString
import static org.cytoscape.model.CyNetwork.NAME
import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.kamnav.common.model.Node

@TupleConstructor
class ExpandNode extends AbstractTask {

    final CyNetworkView cyNv
    final View<CyNode> nodeView
    final WsAPI wsAPI

    def toNode = { CyNetwork cyNetwork, CyNode cyNode ->
        if (!cyNetwork || !cyNode) return null
        def row = cyNetwork.getRow(cyNode)
        if (!row) return null

        new Node(
            row.get("kam.id", String.class),
            fromString(row.get("bel.function", String.class)),
            row.get(NAME, String.class)
        )
    }

    def findNode = { CyNetwork cyNetwork, String label ->
        def table = cyNetwork.defaultNodeTable
        table.getMatchingRows(NAME, label).
            collect { row ->
                long id = row.get(table.primaryKey.name, Long.class)
                if (!id) return null
                cyNetwork.getNode(id)
            }.find()
    }

    def makeNode = { CyNetwork cyN, String id, String fx, String label ->
        def n = cyN.addNode()
        def table = cyN.defaultNodeTable
        table.getColumn('bel.function') ?: table.createColumn('bel.function', String.class, false)
        table.getColumn('kam.id') ?: table.createColumn('kam.id', String.class, false)

        cyN.getRow(n).set("kam.id", id)
        cyN.getRow(n).set("bel.function", fx)
        cyN.getRow(n).set(NAME, label)
        n
    }

    def makeEdge = { CyNetwork cyN, CyNode s, CyNode t, String id, String rel ->
        def table = cyN.defaultEdgeTable
        table.getColumn('kam.id') ?: table.createColumn('kam.id', String.class, false)
        CyEdge e = cyN.addEdge(s, t, true)
        cyN.getRow(e).set('kam.id', rel)
        cyN.getRow(e).set(CyEdge.INTERACTION, rel)
        e
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        def node = toNode.call(cyNv.model, nodeView.model)
        if (!node) {
            monitor.statusMessage = 'The node was not found in the network.'
            cancel()
        }

        monitor.title = format("Expand %s node", node.label)
        monitor.statusMessage = 'Expanding node'

        def edges = wsAPI.adjacentEdges(node)
        def chunk = 1.0d / edges.length
        wsAPI.adjacentEdges(node).each { edge ->
            def s = edge.source
            def t = edge.target
            def cySource = findNode.call(cyNv.model, s.label) ?:
                makeNode.call(cyNv.model, s.id, s.fx.displayValue, s.label)
            def cyTarget =
                findNode.call(cyNv.model, t.label) ?:
                makeNode.call(cyNv.model, t.id, t.fx.displayValue, t.label)
            makeEdge.call(cyNv.model, cySource, cyTarget, edge.id, edge.relationship.displayValue)

            monitor.progress += chunk
        }
    }
}
