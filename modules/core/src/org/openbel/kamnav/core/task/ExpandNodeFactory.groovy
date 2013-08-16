package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.task.AbstractNodeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import org.openbel.ws.api.WsAPI

@TupleConstructor
class ExpandNodeFactory extends AbstractNodeViewTaskFactory {

    final WsAPI wsAPI

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView cyNv) {
        return new TaskIterator(new ExpandNode(cyNv, nodeView, wsAPI))
    }
}
