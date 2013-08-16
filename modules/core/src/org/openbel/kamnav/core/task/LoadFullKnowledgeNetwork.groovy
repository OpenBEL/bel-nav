package org.openbel.kamnav.core.task

import org.cytoscape.application.CyApplicationManager
import org.cytoscape.view.model.CyNetworkView
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.framework.ws.model.FunctionType

import static org.openbel.framework.common.enums.FunctionEnum.fromString
import static org.openbel.kamnav.common.util.NodeUtil.*
import static org.openbel.kamnav.common.util.EdgeUtil.*
import static org.cytoscape.model.CyNetwork.NAME;
import groovy.transform.TupleConstructor
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.ws.api.WsAPI

@TupleConstructor
class LoadFullKnowledgeNetwork extends AbstractTask {

    final CyApplicationManager appMgr
    final WsAPI wsAPI

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        def cyN = appMgr.currentNetwork
        def knName = cyN.getRow(cyN).get(NAME, String.class)
        monitor.title = "Load nodes and edge from ${knName}".toString()

        FunctionType.values().each {
            FunctionEnum fx = fromString(it.displayValue)
            if (!fx) return
            monitor.statusMessage = "Adding ${fx.displayValue} functions".toString()
            wsAPI.findNodes(knName, ~/.*/, fx).
                each { node ->
                    def n = findNode(cyN, node.label)
                    if (!n) {
                        n = makeNode(cyN, node.id, node.fx.displayValue, node.label)

                        wsAPI.adjacentEdges(toNode.call(cyN, n)).each { edge ->
                            def s = edge.source
                            def t = edge.target
                            def cySource = findNode.call(cyN, s.label) ?:
                                makeNode.call(cyN, s.id, s.fx.displayValue, s.label)
                            def cyTarget =
                                findNode.call(cyN, t.label) ?:
                                    makeNode.call(cyN, t.id, t.fx.displayValue, t.label)
                            makeEdge.call(cyN, cySource, cyTarget, edge.id, edge.relationship.displayValue)
                        }
                    }
                }
        }
    }
}
