package org.openbel.kamnav.core.task

import static org.cytoscape.model.CyTableUtil.getNodesInState
import static org.openbel.kamnav.common.util.NodeUtil.toNode
import groovy.transform.TupleConstructor
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyNode
import org.cytoscape.task.AbstractNodeViewTaskFactory
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.TaskIterator
import org.openbel.ws.api.WsAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@TupleConstructor
class ExpandNodeFactory extends AbstractNodeViewTaskFactory {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages");
    final ApplyPreferredLayoutTaskFactory aplFac
    final CyEventHelper evtHelper
    final VisualMappingManager visMgr
    final WsAPI wsAPI

    @Override
    boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
        def node = toNode(networkView.model, nodeView.model)
        if (!node.id) {
            msg.warn("${node.label} is not linked to a Knowledge Network.")
        }
        node.id
    }

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView cyNv) {
        TaskIterator tasks = new TaskIterator(new ExpandNode(cyNv, nodeView, evtHelper, visMgr, wsAPI))
        getNodesInState(cyNv.model, 'selected', true).collect {
            def nodeV = cyNv.getNodeView(it)
            new TaskIterator(new ExpandNode(cyNv, nodeV, evtHelper, visMgr, wsAPI))
        }.each(tasks.&append)
        tasks.append(aplFac.createTaskIterator([cyNv]))
        return tasks
    }
}
