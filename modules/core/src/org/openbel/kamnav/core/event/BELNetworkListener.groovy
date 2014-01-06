package org.openbel.kamnav.core.event

import groovy.transform.TupleConstructor
import org.cytoscape.model.events.AddedNodesEvent
import org.cytoscape.model.events.AddedNodesListener
import org.cytoscape.model.events.NetworkAddedEvent
import org.cytoscape.model.events.NetworkAddedListener
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.task.AddBelColumnsToCurrent

@TupleConstructor
class BELNetworkListener implements NetworkAddedListener, AddedNodesListener {

    final Expando cyr

    @Override
    void handleEvent(NetworkAddedEvent evt) {
        def missingData = evt.network.nodeList.any {
            !evt.network.getRow(it).isSet('bel.function')
        }

        if (missingData) {
            cyr.taskManager.execute(new TaskIterator(new AddBelColumnsToCurrent(null, evt.network)))
        }
    }

    @Override
    void handleEvent(AddedNodesEvent evt) {
    }
}
