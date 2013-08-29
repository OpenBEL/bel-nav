package org.openbel.kamnav.core

import static org.openbel.kamnav.core.Constant.*
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
import org.cytoscape.view.vizmap.VisualMappingManager

class Util {

    def static contributeVisualStyles(VisualMappingManager visMgr,
                               LoadVizmapFileTaskFactory vf) {
        // delete/add knowledge network styles (idempotent)
        visMgr.allVisualStyles.findAll { it.title in STYLE_NAMES }.each(visMgr.&removeVisualStyle)
        vf.loadStyles(Util.class.getResourceAsStream(STYLE_PATH))
    }
}
