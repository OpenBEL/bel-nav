package org.openbel.kamnav.common.util

import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.enums.FunctionEnum.fromString
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.openbel.kamnav.common.model.Node

class NodeUtil {

    static def toNode = { CyNetwork cyNetwork, CyNode cyNode ->
        if (!cyNetwork || !cyNode) return null
        def row = cyNetwork.getRow(cyNode)
        if (!row) return null

        new Node(
                row.get("kam.id", String.class),
                fromString(row.get("bel.function", String.class)),
                row.get(NAME, String.class)
        )
    }

    static def findNode = { CyNetwork cyNetwork, String label ->
        def table = cyNetwork.defaultNodeTable
        table.getMatchingRows(NAME, label).
                collect { row ->
                    long id = row.get(table.primaryKey.name, Long.class)
                    if (!id) return null
                    cyNetwork.getNode(id)
                }.find()
    }

    static def makeNode = { CyNetwork cyN, String id, String fx, String label ->
        def n = cyN.addNode()
        def table = cyN.defaultNodeTable
        table.getColumn('bel.function') ?: table.createColumn('bel.function', String.class, false)
        table.getColumn('kam.id') ?: table.createColumn('kam.id', String.class, false)

        cyN.getRow(n).set("kam.id", id)
        cyN.getRow(n).set("bel.function", fx)
        cyN.getRow(n).set(NAME, label)
        n
    }
}
