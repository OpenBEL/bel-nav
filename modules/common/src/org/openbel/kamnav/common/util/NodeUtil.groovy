package org.openbel.kamnav.common.util

import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.model.Parameter
import org.openbel.framework.common.model.Term
import org.openbel.kamnav.common.model.Node

import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.bel.parser.BELParser.parseTerm
import static org.openbel.framework.common.enums.FunctionEnum.fromString
import static org.openbel.kamnav.common.util.Util.createColumn
import static org.openbel.kamnav.common.util.Util.createListColumn

class NodeUtil {

    static def createNodeColumns(CyNetwork cyN) {
        createColumn(cyN.defaultNodeTable, 'bel.function', String.class, false, null)
        createListColumn(cyN.defaultNodeTable, 'namespace', String.class, false, null)
        createListColumn(cyN.defaultNodeTable, 'entity', String.class, false, null)
        createColumn(cyN.defaultNodeTable, 'kam.id', String.class, false, null)
        createColumn(cyN.defaultNodeTable, 'linked', Boolean.class, false, null)
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

    static def toEntityDisplay(CyNetwork cyN, CyNode node) {
        def label = cyN.getRow(node).get(NAME, String.class)
        try {
            Term t = parseTerm(label)
            if (!t) return label
            t.allParametersLeftToRight.collect { it.value }.join(', ')
        } catch (InvalidArgument e) {
            // parse failure; cannot resolve so return
            return label
        }
    }

    static def decomposeTerm(belTerm) {
        try {
            Term t = parseTerm(belTerm)
            if (!t) return null

            return [
                fx: t.functionEnum.displayValue,
                namespaces: t.allParametersLeftToRight.
                        inject([] as Set) { Set res, Parameter next ->
                            res << next.namespace?.prefix
                        }.findAll().toArray(),
                entities: t.allParametersLeftToRight.inject([] as Set) {
                            Set res, Parameter next ->
                                res << next.value
                        }.findAll().toArray(),
                bel_label: t.toBELShortForm()
            ]
        } catch (InvalidArgument e) {
            // indicates failure to parse; skip
            return null
        }
    }
}
