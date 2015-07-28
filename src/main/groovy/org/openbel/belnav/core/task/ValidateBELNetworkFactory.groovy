package org.openbel.belnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.event.CyEventHelper
import org.cytoscape.task.AbstractNetworkViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.view.vizmap.VisualStyleFactory
import org.cytoscape.work.TaskIterator

/**
 * ValidateBELNetworkFactory provides a {@link org.cytoscape.work.TaskFactory} for
 * validation of BEL nodes an edges of a {@link CyNetworkView}.
 */
@TupleConstructor
class ValidateBELNetworkFactory extends AbstractNetworkViewTaskFactory {

    final CyEventHelper cyEventHelper
    final VisualStyleFactory vsFac
    final VisualMappingManager vmManager
    final VisualMappingFunctionFactory discreteFxFac

    @Override
    TaskIterator createTaskIterator(CyNetworkView cyNv) {
        new TaskIterator(new ValidateBELNetwork(cyNv, cyEventHelper, vsFac, vmManager, discreteFxFac))
    }
}
