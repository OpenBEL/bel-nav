package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.application.swing.CytoPanelName
import org.cytoscape.application.swing.CytoPanelState
import org.cytoscape.model.CyEdge
import org.cytoscape.task.EdgeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.Task
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.ui.Updateable
import static Boolean.TRUE
import static org.cytoscape.application.swing.CytoPanelName.*
import static org.cytoscape.application.swing.CytoPanelState.*

@TupleConstructor
class ShowEvidenceFactory implements EdgeViewTaskFactory {

    final Expando cyr
    final Updateable updateable

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
        // update evidence
        def cyE = edgeV.model
        def evTbl = cyr.cyTableManager.getAllTables(true).find { it.title == 'BEL.Evidence' }
        def evidence = evTbl.getMatchingRows('edge', cyE.SUID)
        updateable.update(evidence.collect { row ->
            [
                    statement: row.get('statement', String.class),
                    citation: [
                            name: row.get('citation name', String.class),
                            type: row.get('citation type', String.class),
                            id: row.get('citation id', String.class)
                    ],
                    annotations: row.allValues.findAll { k, v ->
                        k.startsWith('annotation: ')
                    }.collectEntries { k, v ->
                        [k.replaceFirst('annotation: ', ''), v]
                    }
            ]
        })

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
