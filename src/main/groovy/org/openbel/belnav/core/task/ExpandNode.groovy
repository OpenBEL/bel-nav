package org.openbel.belnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskMonitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.openbel.belnav.common.util.EdgeUtil.findEdge
import static org.openbel.belnav.common.util.EdgeUtil.makeEdge
import static org.openbel.belnav.common.util.NodeUtil.*
import static org.openbel.belnav.core.Util.addEvidenceForEdge

@TupleConstructor
class ExpandNode extends BaseTask {

    private static final Logger msg = LoggerFactory.getLogger('CyUserMessages');
    final CyNetworkView cyNv
    final View<CyNode> nodeView
    final Expando cyr

    /**
     * {@inheritDoc}
     */
    @Override
    void doRun(TaskMonitor m) throws Exception {
        def wsAPI = cyr.wsManager.get(cyr.wsManager.default)

        def node = toNode(cyNv.model, nodeView.model)
        m.title = 'Expand Node'
        m.statusMessage = "Expanding ${node.label}"

        def edges = wsAPI.adjacentEdges(node)
        def chunk = 1.0d / edges.length
        wsAPI.adjacentEdges(node).each { edge ->
            def s = edge.source
            def t = edge.target
            def rel = edge.relationship
            def cySource = findNode(cyNv.model, s.label) ?:
                makeNode(cyNv.model, s.id, s.fx.displayValue, s.label)
            def cyTarget =
                findNode(cyNv.model, t.label) ?:
                makeNode(cyNv.model, t.id, t.fx.displayValue, t.label)
            def cyE = findEdge(cyNv.model, s.label, rel, t.label) ?:
                makeEdge(cyNv.model, cySource, cyTarget, edge.id, rel)
            addEvidenceForEdge(cyr.cyTableManager, cyr.cyTableFactory, wsAPI, cyNv.model, cyE)
            m.progress += chunk
        }

        cyr.cyEventHelper.flushPayloadEvents()
        cyr.visualMappingManager.getCurrentVisualStyle().apply(cyNv)
        cyNv.updateView()

        msg.info("Expanded ${node.label}")
    }
}
