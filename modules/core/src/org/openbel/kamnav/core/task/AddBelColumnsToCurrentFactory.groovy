package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.event.CyEventHelper
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.AbstractTaskFactory
import org.cytoscape.work.TaskIterator

@TupleConstructor
class AddBelColumnsToCurrentFactory extends AbstractTaskFactory {

    final CyApplicationManager appMgr
    final CyEventHelper evtHelper
    final VisualMappingManager visMgr

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
            new AddBelColumnsToCurrent(appMgr),
            new ApplyPreferredStyleToCurrent(appMgr, evtHelper, visMgr))
    }
}
