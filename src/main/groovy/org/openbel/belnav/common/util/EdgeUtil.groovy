package org.openbel.belnav.common.util

import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.openbel.framework.common.enums.RelationshipType
import org.openbel.belnav.common.model.Edge

import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.model.CyEdge.INTERACTION
import static org.cytoscape.model.CyEdge.Type.DIRECTED
import static org.openbel.framework.common.enums.RelationshipType.fromAbbreviation
import static org.openbel.framework.common.enums.RelationshipType.fromString
import static org.openbel.belnav.common.util.NodeUtil.findNode
import static org.openbel.belnav.common.util.NodeUtil.toNode

class EdgeUtil {

    static final Set<String> relationships = []
    static {
        relationships.addAll(RelationshipType.values().collect { it.displayValue }.findAll())
        relationships.addAll(RelationshipType.values().collect { it.abbreviation }.findAll())
    }

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

    static def findEdge(CyNetwork cyN, String label) {
        if (!label) return null

        def rm = relationships.find {
            label.indexOf(it) != -1
        }
        if (!rm) return null

        def relStart = label.indexOf(rm)
        def objStart = relStart + rm.length()
        findEdge(cyN,
                label[0..<relStart].trim(),             // subject
                label[relStart..rm.length()+1].trim(),  // relationship
                label[objStart..-1].trim())             // object
    }

    static def makeEdge(CyNetwork cyN, CyNode s, CyNode t, String id, String rel) {
        def table = cyN.defaultEdgeTable
        table.getColumn('kam.id') ?: table.createColumn('kam.id', String.class, false)
        table.getColumn('linked') ?: table.createColumn('linked', Boolean.class, false)
        CyEdge e = cyN.addEdge(s, t, true)
        def row = cyN.getRow(e)
        def lbl = computeEdgeLabel(cyN, e)
        row.set(NAME, lbl)
        row.set("shared name", lbl)
        row.set('linked', true)
        row.set('kam.id', id)
        row.set(INTERACTION, rel)
        e
    }

    static def toEdge(CyNetwork cyN, CyEdge cyE) {
        if (!cyN || !cyE) return null
        def row = cyN.getRow(cyE)
        if (!row) return null

        new Edge(
            row.get('kam.id', String.class),
            toNode(cyN, cyE.source),
            row.get(INTERACTION, String.class),
            toNode(cyN, cyE.target)
        )
    }

    static def computeEdgeLabel(CyNetwork cyN, CyEdge cyE) {
        if (!cyN || !cyE) return null
        def row = cyN.getRow(cyE)
        if (!row) return null

        def values = [
            cyN.getRow(cyE.source).get(NAME, String.class),
            row.get(INTERACTION, String.class),
            cyN.getRow(cyE.target).get(NAME, String.class)
        ]

        if (!values.every()) return null
        values.join(' ')
    }

    static def findEdgeBySUID(long suid, Set<CyNetwork> networks) {
        for (CyNetwork cyN : networks) {
            CyEdge cyE = cyN.getEdge(suid)
            if(cyE) return [cyN, cyE]
        }
        null
    }
}
