package org.openbel.kamnav.core.event

import groovy.transform.TupleConstructor
import org.cytoscape.session.events.SessionLoadedEvent
import org.cytoscape.session.events.SessionLoadedListener
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
import org.cytoscape.view.vizmap.VisualMappingManager

import static org.openbel.kamnav.core.Util.contributeVisualStyles

@TupleConstructor
class SessionLoadListener implements SessionLoadedListener {

    final VisualMappingManager visMgr
    final LoadVizmapFileTaskFactory vf

    @Override
    void handleEvent(SessionLoadedEvent ev) {
        contributeVisualStyles(visMgr, vf)
    }
}
