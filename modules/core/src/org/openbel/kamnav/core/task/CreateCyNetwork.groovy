package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor

import static java.lang.String.format
import static org.cytoscape.model.CyNetwork.NAME
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import org.openbel.ws.api.WsAPI

@TupleConstructor
class CreateCyNetwork extends AbstractTask {

    final CyApplicationManager appMgr
    final CyNetworkFactory cynFac
    final CyNetworkViewFactory cynvFac
    final CyNetworkManager cynMgr
    final CyNetworkViewManager cynvMgr
    final WsAPI wsAPI

    private String knName
    private ListSingleSelection<String> knNameSelection

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
    @Override
    void run(TaskMonitor monitor) throws Exception {
        monitor.title = "Load knowledge network for $knName".toString()
        monitor.statusMessage = format("Loading %s", knName)
        monitor.progress = -1;
        Map load = wsAPI.loadKnowledgeNetwork(knName)
        if (!load.handle) {
            monitor.statusMessage = load.message
            return
        }

        CyNetwork network = cynFac.createNetwork()
        network.getRow(network).set(NAME, knName)
        cynMgr.addNetwork(network)
        def view = cynvFac.createNetworkView(network)
        cynvMgr.addNetworkView(view)
        appMgr.currentNetwork = network
        appMgr.currentNetworkView = view
    }
}
