package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.framework.ws.model.FunctionType
import org.openbel.ws.api.WsAPI

import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.enums.FunctionEnum.fromString
import static org.openbel.kamnav.common.util.EdgeUtil.findEdge
import static org.openbel.kamnav.common.util.EdgeUtil.makeEdge
import static org.openbel.kamnav.common.util.NodeUtil.*

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
        monitor.title = "Load network for ${knName}"
        def progress = 0.0d
        monitor.progress = progress

        def nodes = []
        def chunk = 0.5d / FunctionType.values().length
        FunctionType.values().each {
            FunctionEnum fx = fromString(it.displayValue)
            if (!fx) return
            monitor.statusMessage = "Adding ${fx.displayValue} functions"
            nodes.addAll(wsAPI.findNodes(knName, ~/.*/, fx).
                collect { node ->
                    def n = findNode(cyN, node.label)
                    if (!n) {
                        n = makeNode(cyN, node.id, node.fx.displayValue, node.label)
                    }
                    n
                })
            monitor.progress = (progress += chunk)
        }
        monitor.progress = (progress = 0.5d)

        chunk = 0.5d / nodes.size()
        monitor.statusMessage = 'Adding adjacent edges'
        nodes.each { n ->
            wsAPI.adjacentEdges(toNode(cyN, n)).each { edge ->
                def s = edge.source
                def t = edge.target
                def r = edge.relationship.displayValue
                def cySource = findNode(cyN, s.label) ?:
                    makeNode(cyN, s.id, s.fx.displayValue, s.label)
                def cyTarget =
                    findNode(cyN, t.label) ?:
                        makeNode(cyN, t.id, t.fx.displayValue, t.label)
                findEdge(cyN, s.label, r, t.label) ?:
                    makeEdge(cyN, cySource, cyTarget, edge.id, r)
            }
            monitor.progress = (progress += chunk)
        }
    }
}
