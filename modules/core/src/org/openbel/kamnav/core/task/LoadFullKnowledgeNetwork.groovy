package org.openbel.kamnav.core.task

import org.cytoscape.model.CyEdge
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static java.lang.String.format
import static org.cytoscape.model.CyNetwork.NAME;
import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.ws.api.WsAPI

@TupleConstructor
class LoadFullKnowledgeNetwork extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(getClass())
    final CyNetworkFactory cynFac
    final CyNetworkViewFactory cynvFac
    final CyNetworkManager cynMgr
    final CyNetworkViewManager cynvMgr
    final WsAPI wsAPI

    // tunable
    private ListSingleSelection<String> knName

    @Tunable(description = "Knowledge network")
    ListSingleSelection<String> getKnName() {
        knName = knName ?: new ListSingleSelection<String>(wsAPI.knowledgeNetworks().keySet().sort())
    }

    void setKnName(ListSingleSelection<String> lsel) {
        this.knName = lsel
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        CyNetwork network = cynFac.createNetwork()
        network.getRow(network).set(NAME, knName.selectedValue)
        CyNode a = network.addNode()
        CyNode b = network.addNode()
        network.getRow(a).set(NAME, "p(HGNC:AKT1)")
        network.getRow(b).set(NAME, "p(HGNC:TNF)")

        CyEdge a_b = network.addEdge(a, b, true)
        network.getRow(a_b).set(NAME, "increases")

        cynMgr.addNetwork(network)
        cynvMgr.addNetworkView(cynvFac.createNetworkView(network))
    }
}
