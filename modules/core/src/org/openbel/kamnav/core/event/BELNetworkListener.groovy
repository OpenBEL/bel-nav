package org.openbel.kamnav.core.event

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.model.CyNode
import org.cytoscape.model.events.NetworkAddedEvent
import org.cytoscape.model.events.NetworkAddedListener
import org.cytoscape.model.events.RowSetRecord
import org.cytoscape.model.events.RowsSetEvent
import org.cytoscape.model.events.RowsSetListener
import org.cytoscape.work.TaskIterator
import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.model.Statement
import org.openbel.framework.common.model.Term
import org.openbel.kamnav.core.task.AddBelColumnsToCurrent
import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.model.CyEdge.INTERACTION
import static org.openbel.framework.common.bel.parser.BELParser.parseStatement
import static org.openbel.framework.common.bel.parser.BELParser.parseTerm
import static org.openbel.kamnav.common.util.EdgeUtil.computeEdgeLabel
import static org.openbel.kamnav.common.util.NodeUtil.findNodeBySUID
import static org.cytoscape.model.CyEdge.Type.ANY

@TupleConstructor
class BELNetworkListener implements NetworkAddedListener, RowsSetListener {

    private static final String SUID = "SUID"

    final Expando cyr

    @Override
    void handleEvent(NetworkAddedEvent evt) {
        def missingData =
            evt.network.nodeList.empty ||
            evt.network.nodeList.any {
                !evt.network.getRow(it).isSet('bel.function')
            }

        if (missingData) {
            cyr.taskManager.execute(new TaskIterator(new AddBelColumnsToCurrent(null, evt.network)))
        }
    }

    @Override
    void handleEvent(RowsSetEvent evt) {
        if (!evt.containsColumn(NAME)) return

        evt.getColumnRecords(NAME).each {
            setFunction(cyr.cyNetworkManager, it)
            setInteraction(it)
        }
    }

    private static void setFunction(CyNetworkManager cyNetworkManager, RowSetRecord record) {
        def tbl = record.row.table
        if (tbl.title?.contains("default node") && tbl.getColumn('bel.function')) {
            def name = record.row.get(NAME, String.class)
            if (name) {
                try {
                    Term t = parseTerm(name)
                    if (!t) {
                        // indicates failure to parse; skip
                        return
                    }

                    record.row.set('bel.function', t.functionEnum.displayValue)

                    Long suid = record.row.get(SUID, Long.class)
                    if (suid) {
                        def match = findNodeBySUID(suid, cyNetworkManager.networkSet)
                        def (CyNetwork cyN, CyNode cyNode) = match
                        cyN.getAdjacentEdgeList(cyNode, ANY).each { adjEdge ->
                            def edgeLabel = computeEdgeLabel(cyN, adjEdge)
                            cyN.getRow(adjEdge).set(NAME, edgeLabel)
                        }
                    }
                } catch (InvalidArgument e) {
                    // indicates failure to parse; skip
                }
            }
        }
    }

    private static void setInteraction(RowSetRecord record) {
        def tbl = record.row.table
        if (tbl.title?.contains("default edge") && tbl.getColumn(INTERACTION)) {
            def name = record.row.get(NAME, String.class)
            if (name) {
                try {
                    Statement stmt = parseStatement(name)
                    if (!stmt) {
                        // indicates failure to parse; skip
                        return
                    }

                    if (stmt.relationshipType) {
                        record.row.set(INTERACTION, stmt.relationshipType.displayValue)
                    }
                } catch (InvalidArgument e) {
                    // indicates failure to parse; skip
                }
            }
        }
    }
}
