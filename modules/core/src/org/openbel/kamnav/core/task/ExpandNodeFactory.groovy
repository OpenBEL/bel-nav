package org.openbel.kamnav.core.task

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

@TupleConstructor
class ExpandNodeFactory extends AbstractNodeViewTaskFactory {

    final ApplyPreferredLayoutTaskFactory aplFac
    final CyEventHelper evtHelper
    final VisualMappingManager visMgr
    final WsAPI wsAPI

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView cyNv) {
        TaskIterator tasks = new TaskIterator(new ExpandNode(cyNv, nodeView, evtHelper, visMgr, wsAPI))
        tasks.append(aplFac.createTaskIterator([cyNv]))
        return tasks
    }
}
