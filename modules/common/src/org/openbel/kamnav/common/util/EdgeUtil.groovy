package org.openbel.kamnav.common.util

import static org.openbel.framework.common.enums.RelationshipType.fromAbbreviation
import static org.openbel.framework.common.enums.RelationshipType.fromString
import static org.cytoscape.model.CyEdge.INTERACTION
import static org.cytoscape.model.CyEdge.Type.DIRECTED
import static org.openbel.kamnav.common.util.NodeUtil.findNode
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.openbel.framework.common.enums.RelationshipType

class EdgeUtil {

    static def createEdgeColumns(CyNetwork cyN) {
        cyN.defaultEdgeTable.getColumn('kam.id') ?:
            cyN.defaultEdgeTable.createColumn('kam.id', String.class, false)
        cyN.defaultEdgeTable.getColumn('linked') ?:
            cyN.defaultEdgeTable.createColumn('linked', Boolean.class, false)
    }

    static def findEdge(CyNetwork cyNetwork, String source, String rel, String target) {
        def nodeSource = findNode(cyNetwork, source)
        if (!nodeSource) return null
        def nodeTarget = findNode(cyNetwork, target)
        if (!nodeTarget) return null
        RelationshipType rtype = fromString(rel)
        if (!rtype) rtype = fromAbbreviation(rel)
        if (!rtype) return null

        cyNetwork.getConnectingEdgeList(nodeSource, nodeTarget, DIRECTED).find {
            def row = cyNetwork.getRow(it)
            row.get(INTERACTION, String.class) == rtype.displayValue
        }
    }

    static def makeEdge(CyNetwork cyN, CyNode s, CyNode t, String id, String rel) {
        def table = cyN.defaultEdgeTable
        table.getColumn('kam.id') ?: table.createColumn('kam.id', String.class, false)
        CyEdge e = cyN.addEdge(s, t, true)
        cyN.getRow(e).set('kam.id', id)
        cyN.getRow(e).set(INTERACTION, rel)
        e
    }
}
