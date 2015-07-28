package org.openbel.belnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.work.AbstractTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.belnav.core.AddBelColumnsToCurrentFactory

@TupleConstructor
class AddBelColumnsToCurrentFactoryImpl extends AbstractTaskFactory
    implements AddBelColumnsToCurrentFactory {

    final Expando cyr

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
            new AddBelColumnsToCurrent(cyr.cyApplicationManager, null),
            new ApplyPreferredStyleToCurrent(cyr.cyApplicationManager,
                    cyr.cyEventHelper, cyr.visualMappingManager))
    }
}
