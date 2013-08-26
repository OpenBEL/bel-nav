package org.openbel.kamnav.core.task

import org.cytoscape.view.vizmap.VisualStyle

import static org.openbel.kamnav.core.Constant.NAV_VIS
import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.event.CyEventHelper
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

@TupleConstructor
public class ApplyPreferredStyleToCurrent extends AbstractTask {

    final CyApplicationManager appMgr
    final CyEventHelper evtHelper
    final VisualMappingManager visMgr

	@Override
	public void run(TaskMonitor monitor) {
        CyNetworkView view = appMgr.currentNetworkView
        if (!view) return

        evtHelper.flushPayloadEvents()
        VisualStyle vs = visMgr.allVisualStyles.groupBy {
            it.title
        }[NAV_VIS].first()
        vs.apply(view)
        view.updateView()
	}
}
