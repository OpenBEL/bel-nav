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

class LinkKnowledgeNetwork extends AbstractTask {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages");
    private static Logger log = LoggerFactory.getLogger(getClass())

    private final CyApplicationManager appMgr
    private final WsAPI wsAPI
    private final CyNetworkView cyNv

    // tunable state
    private String knName
    private ListSingleSelection<String> knNameSelection

    private LinkKnowledgeNetwork(final CyApplicationManager appMgr,
            final WsAPI wsAPI, final CyNetworkView cyNv) {
        this.appMgr = appMgr
        this.wsAPI = wsAPI
        this.cyNv = cyNv
    }

    // Called by cytoscape
    @Tunable(description = "Knowledge network")
    public ListSingleSelection<String> getKnName() {
        knNameSelection = knNameSelection ?:
            new ListSingleSelection<String>(wsAPI.knowledgeNetworks().keySet() as String[])
    }

    // Called by cytoscape
    public void setKnName(ListSingleSelection<String> lsel) {
        this.knName = lsel.selectedValue
    }

    /**
     * {@inheritDoc}
     */
    @Override void run(TaskMonitor monitor) throws Exception {
        def cyN = cyNv.model
        if (!cyN.nodeCount) {
            msg.error("0 nodes in network.")
            return
        }
        def name = cyN.getRow(cyN).get(NAME, String.class)
        monitor.title = "Link $name (Network) to $knName (Knowledge Network)"

        monitor.statusMessage = "Loading $knName."
        wsAPI.loadKnowledgeNetwork(knName)

        monitor.statusMessage = "Resolving nodes to $knName"
        def nodeCount = wsAPI.linkNodes(cyNv.model, knName).count {it}
        monitor.statusMessage = "Resolving edges to $knName"
        def edgeCount = wsAPI.linkEdges(cyNv.model, knName).count {it}
        msg.info("Linked ${nodeCount} nodes and ${edgeCount} edges.")
    }
}
