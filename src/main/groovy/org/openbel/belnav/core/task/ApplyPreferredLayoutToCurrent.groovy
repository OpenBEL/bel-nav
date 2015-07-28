package org.openbel.belnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.view.layout.CyLayoutAlgorithm
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskMonitor

@TupleConstructor
public class ApplyPreferredLayoutToCurrent extends BaseTask {

    private static final String DEF_LAYOUT = "force-directed"
    private static final String PL_PROP = "preferredLayoutAlgorithm"

    final CyApplicationManager appMgr
    final CyLayoutAlgorithmManager layouts
    final Properties props

	@Override
	public void doRun(TaskMonitor monitor) {
        CyNetworkView view = appMgr.currentNetworkView
        if (!view) return

		monitor.progress = 0.0d
		monitor.statusMessage = "Applying Default Layout..."

        String pref =
            props?.getProperty(PL_PROP, DEF_LAYOUT) ?:
            CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME
        monitor.progress = 0.2d

        final CyLayoutAlgorithm layout = layouts.getLayout(pref)
        if (layout != null) {
            final TaskIterator itr = layout.createTaskIterator(view, layout.getDefaultLayoutContext(),
                    CyLayoutAlgorithm.ALL_NODE_VIEWS, "")
            insertTasksAfterCurrentTask(itr)
        } else {
            throw new IllegalArgumentException("Couldn't find layout algorithm: " + pref)
        }
	    monitor.progress = 1.0d
	}
}
