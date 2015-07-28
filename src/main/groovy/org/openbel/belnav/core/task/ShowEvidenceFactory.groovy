package org.openbel.belnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.model.CyEdge
import org.cytoscape.task.EdgeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.Task
import org.cytoscape.work.TaskIterator
import org.openbel.belnav.ui.EdgeUpdateable

import static Boolean.TRUE
import static org.cytoscape.application.swing.CytoPanelName.EAST
import static org.cytoscape.application.swing.CytoPanelState.DOCK

@TupleConstructor
class ShowEvidenceFactory implements EdgeViewTaskFactory {

    final Expando cyr
    final EdgeUpdateable updateable

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isReady(View<CyEdge> edgeV, CyNetworkView cyNv) {
        def row = cyNv.model.getRow(edgeV.model)
        return row.isSet('linked') && row.get('linked', Boolean.class) == TRUE
    }

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(View<CyEdge> edgeV, CyNetworkView cyNv) {
        // show evidence panel in "Results Panel" (east cyto panel)
        def east = cyr.cySwingApplication.getCytoPanel(EAST)
        east.state = DOCK
        def idx = east.indexOfComponent(((CytoPanelComponent) updateable).component)
        east.selectedIndex = idx

        // empty task iterator
        return new TaskIterator({
            run: {}
            cancel: {}
        } as Task)
    }
}
