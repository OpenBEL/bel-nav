package org.openbel.kamnav.core.task

import static org.cytoscape.model.CyNetwork.NAME
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import org.openbel.ws.api.WsAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static java.lang.String.format

class LinkKnowledgeNetwork extends AbstractTask {

    private static Logger log = LoggerFactory.getLogger(getClass())

    private final CyApplicationManager appMgr
    private final WsAPI wsAPI
    private final CyNetworkView cyNv

    // tunable state
    private String knName

    private LinkKnowledgeNetwork(final CyApplicationManager appMgr,
            final WsAPI wsAPI, final CyNetworkView cyNv) {
        this.appMgr = appMgr
        this.wsAPI = wsAPI
        this.cyNv = cyNv
    }

    // Called by cytoscape
    @Tunable(description = "Knowledge network")
    public ListSingleSelection<String> getKnName() {
        String[] names = wsAPI.knowledgeNetworks().keySet() as String[]
        return new ListSingleSelection<String>(names)
    }

    // Called by cytoscape
    public void setKnName(ListSingleSelection<String> lsel) {
        this.knName = lsel.selectedValue
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        monitor.title = format("Link Current Network to %s", knName)

        monitor.statusMessage = format("Loading %s.", knName)
        wsAPI.loadKnowledgeNetwork(knName)

        monitor.statusMessage = format("Resolving nodes to %s", knName)
        def chunk = 1d / cyNv.model.nodeCount
        wsAPI.link(cyNv.model, knName) { node, mapping ->
            String name = cyNv.model.getRow(node).get(NAME, String.class)
            log.info("resolved ${name} to $mapping".toString())
            monitor.progress += chunk
        }
    }
}
