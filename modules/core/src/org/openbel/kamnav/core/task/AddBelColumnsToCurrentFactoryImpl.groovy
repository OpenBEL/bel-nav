package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.event.CyEventHelper
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.AbstractTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory

@TupleConstructor
class AddBelColumnsToCurrentFactoryImpl extends AbstractTaskFactory
    implements AddBelColumnsToCurrentFactory {

    final Expando cyr

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
            new AddBelColumnsToCurrent(cyr.cyApplicationManager),
            new ApplyPreferredStyleToCurrent(cyr.cyApplicationManager,
                    cyr.cyEventHelper, cyr.visualMappingManager))
    }
}
