package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.task.AbstractNetworkViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.TaskIterator

@TupleConstructor
class ViewAsEntityFactory extends AbstractNetworkViewTaskFactory {

    final VisualMappingManager vmManager
    final VisualMappingFunctionFactory vmFxFactory

    @Override
    TaskIterator createTaskIterator(CyNetworkView cyNv) {
        new TaskIterator(new ViewAsEntity(cyNv, vmManager, vmFxFactory))
    }
}
