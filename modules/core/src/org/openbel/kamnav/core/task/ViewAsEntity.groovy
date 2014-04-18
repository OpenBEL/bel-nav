package org.openbel.kamnav.core.task

import groovy.transform.PackageScope
import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.presentation.property.BasicVisualLexicon
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.view.vizmap.mappings.PassthroughMapping
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.kamnav.common.util.NodeUtil

import static org.openbel.kamnav.common.util.Util.createColumn
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_LABEL

@TupleConstructor
class ViewAsEntity extends AbstractTask {

    final CyNetworkView cyNv
    final VisualMappingManager vmManager
    final VisualMappingFunctionFactory vmFxFactory

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        def cyN = cyNv.model
        createColumn(cyN.defaultNodeTable, 'entity', String.class, false, null)

        cyN.nodeList.collect { node ->
            [
                node,
                NodeUtil.toEntityDisplay(cyN, node)
            ]
        }.each {
            def (node, entityDisplay) = it
            cyN.getRow(node).set('entity', entityDisplay)
        }

        def entityLabelling = vmFxFactory.createVisualMappingFunction(
                'entity display', String.class, NODE_LABEL) as PassthroughMapping
        vmManager.getVisualStyle(cyNv).addVisualMappingFunction(entityLabelling)
    }
}
