package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.openbel.kamnav.common.util.EdgeUtil.findEdge
import static org.openbel.kamnav.common.util.EdgeUtil.makeEdge
import static org.openbel.kamnav.common.util.NodeUtil.*
import static org.openbel.kamnav.core.Util.addEvidenceForEdge

@TupleConstructor
class ExpandNode extends AbstractTask {

    private static final Logger msg = LoggerFactory.getLogger('CyUserMessages');
    final CyNetworkView cyNv
    final View<CyNode> nodeView
    final Expando cyr

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        def node = toNode(cyNv.model, nodeView.model)
        monitor.title = 'Expand Node'
        monitor.statusMessage = "Expanding ${node.label}"

        def edges = cyr.wsAPI.adjacentEdges(node)
        def chunk = 1.0d / edges.length
        cyr.wsAPI.adjacentEdges(node).each { edge ->
            def s = edge.source
            def t = edge.target
            def rel = edge.relationship.displayValue
            def cySource = findNode(cyNv.model, s.label) ?:
                makeNode(cyNv.model, s.id, s.fx.displayValue, s.label)
            def cyTarget =
                findNode(cyNv.model, t.label) ?:
                makeNode(cyNv.model, t.id, t.fx.displayValue, t.label)
            def cyE = findEdge(cyNv.model, s.label, rel, t.label) ?:
                makeEdge(cyNv.model, cySource, cyTarget, edge.id, rel)
            addEvidenceForEdge(cyr.cyTableManager, cyr.cyTableFactory, cyr.wsAPI, cyNv.model, cyE)
            monitor.progress += chunk
        }

        cyr.cyEventHelper.flushPayloadEvents()
        cyr.visualMappingManager.getCurrentVisualStyle().apply(cyNv)
        cyNv.updateView()

        msg.info("Expanded ${node.label}")
    }
}
