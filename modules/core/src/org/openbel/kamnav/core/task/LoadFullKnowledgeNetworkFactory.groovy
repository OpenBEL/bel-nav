package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.work.AbstractTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.ws.api.WsAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@TupleConstructor
class LoadFullKnowledgeNetworkFactory extends AbstractTaskFactory {

    private static final Logger log = LoggerFactory.getLogger(getClass())
    final CyApplicationManager appMgr
    final CyNetworkFactory cynFac
    final CyNetworkViewFactory cynvFac
    final CyNetworkManager cynMgr
    final CyNetworkViewManager cynvMgr
    final WsAPI wsAPI

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator() {
        log.info("Create new LoadFullKnowledgeNetwork task.")

        return new TaskIterator(
            new CreateCyNetwork(appMgr, cynFac, cynvFac, cynMgr, cynvMgr, wsAPI),
            new LoadFullKnowledgeNetwork(appMgr, wsAPI))
    }
}
