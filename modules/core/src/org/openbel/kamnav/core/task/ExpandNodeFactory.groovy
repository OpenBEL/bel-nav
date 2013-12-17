package org.openbel.kamnav.core.task

import static java.lang.Boolean.TRUE
import static org.cytoscape.model.CyTableUtil.getNodesInState
import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.task.AbstractNodeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator

@TupleConstructor
class ExpandNodeFactory extends AbstractNodeViewTaskFactory {

    final Expando cyr

    @Override
    boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
        // at least one linked node needs to be selected; the unlinked nodes
        // will be filtered once the task executes.
        def selected = getNodesInState(networkView.model, 'selected', true)
        selected.find { node ->
            def row = networkView.model.getRow(node)
            row.isSet('linked') && row.get('linked', Boolean.class) == TRUE
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView cyNv) {
        TaskIterator tasks = new TaskIterator()
        getNodesInState(cyNv.model, 'selected', true).findAll { node ->
            def row = cyNv.model.getRow(node)
            row.isSet('linked') && row.get('linked', Boolean.class) == TRUE
        }.collect {
            def nodeV = cyNv.getNodeView(it)
            new TaskIterator(new ExpandNode(cyNv, nodeV, cyr.cyEventHelper, cyr.visualMappingManager, cyr.wsAPI))
        }.each(tasks.&append)
        return tasks
    }
}
