package org.openbel.kamnav.common.util

import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.model.Term

import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.enums.FunctionEnum.fromString
import static org.openbel.framework.common.bel.parser.BELParser.parseTerm
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.openbel.kamnav.common.model.Node

class NodeUtil {

    static def createNodeColumns(CyNetwork cyN) {
        cyN.defaultNodeTable.getColumn('bel.function') ?:
            cyN.defaultNodeTable.createColumn('bel.function', String.class, false)
        cyN.defaultNodeTable.getColumn('kam.id') ?:
            cyN.defaultNodeTable.createColumn('kam.id', String.class, false)
        cyN.defaultNodeTable.getColumn('linked') ?:
            cyN.defaultNodeTable.createColumn('linked', Boolean.class, false)
    }

    static def toNode(CyNetwork cyNetwork, CyNode cyNode) {
        if (!cyNetwork || !cyNode) return null
        def row = cyNetwork.getRow(cyNode)
        if (!row) return null

        new Node(
            row.get('kam.id', String.class),
            fromString(row.get('bel.function', String.class)),
            row.get(NAME, String.class)
        )
    }

    static def findNode(CyNetwork cyNetwork, String label) {
        def table = cyNetwork.defaultNodeTable
        table.getMatchingRows(NAME, label).
            collect { row ->
                long id = row.get(table.primaryKey.name, Long.class)
                if (!id) return null
                cyNetwork.getNode(id)
            }.find()
    }

    static def makeNode(CyNetwork cyN, String id, String fx, String label) {
        def n = cyN.addNode()
        createNodeColumns(cyN)

        cyN.getRow(n).set('linked', true)
        cyN.getRow(n).set('kam.id', id)
        cyN.getRow(n).set('bel.function', fx)
        cyN.getRow(n).set(NAME, label)
        n
    }

    static def toBEL(CyNetwork cyN, CyNode node) {
        def label = cyN.getRow(node).get(NAME, String.class)
        try {
            Term term = parseTerm(label)
            if (!term) return [:]
            return [fx: term.functionEnum, lbl: label]
        } catch (InvalidArgument e) {
            // parse failure; cannot resolve so return
            return [:]
        }
    }
}
