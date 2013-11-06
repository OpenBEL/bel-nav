package org.openbel.kamnav.core

import org.cytoscape.application.swing.AbstractCyAction
import org.cytoscape.application.swing.CyAction
import org.cytoscape.application.swing.CySwingApplication
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.session.events.SessionLoadedListener
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.kamnav.common.model.Namespace
import org.openbel.kamnav.core.event.SessionLoadListener
import org.openbel.kamnav.core.task.AddBelColumnsToCurrentFactoryImpl
import org.openbel.kamnav.core.task.KnowledgeNeighborhoodFactory
import org.openbel.kamnav.ui.SearchNeighborhoodUI
import org.openbel.kamnav.ui.SearchNodesDialogUI

import java.awt.event.ActionEvent
import java.util.regex.Pattern

import static javax.swing.KeyStroke.getKeyStroke
import static org.openbel.kamnav.common.util.EdgeUtil.findEdge
import static org.openbel.kamnav.common.util.EdgeUtil.makeEdge
import static org.openbel.kamnav.common.util.NodeUtil.findNode
import static org.openbel.kamnav.common.util.NodeUtil.makeNode
import static org.openbel.kamnav.common.util.NodeUtil.toNode
import static org.openbel.kamnav.core.Util.*
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.property.CyProperty
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.task.NetworkViewTaskFactory
import org.cytoscape.task.NodeViewTaskFactory
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.TaskFactory
import org.openbel.kamnav.core.task.ExpandNodeFactory
import org.openbel.kamnav.core.task.LinkKnowledgeNetworkFactory
import org.openbel.kamnav.core.task.LoadFullKnowledgeNetworkFactory
import org.openbel.ws.api.WsAPI
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        def cyr = cyReference(bc, this.&getService, [CyApplicationManager.class,
                CySwingApplication.class, CyNetworkFactory.class, CyNetworkManager.class,
                CyNetworkViewFactory.class, CyNetworkViewManager.class,
                CyLayoutAlgorithmManager.class, VisualMappingManager.class, CyEventHelper.class,
                ApplyPreferredLayoutTaskFactory.class, WsAPI.class] as Class<?>[])
        CyProperty<Properties> cyProp = getService(bc,CyProperty.class,"(cyPropertyName=cytoscape3.props)");
        SearchNodesDialogUI searchNodesUI = getService(bc, SearchNodesDialogUI.class)
        SearchNeighborhoodUI searchKnUI = getService(bc, SearchNeighborhoodUI.class)

        // register listeners
        LoadVizmapFileTaskFactory vf =  getService(bc,LoadVizmapFileTaskFactory.class)
        registerService(bc,
            new SessionLoadListener(cyr.visualMappingManager, vf),
            SessionLoadedListener.class, [:] as Properties)

        // register tasks
        // TODO do not add AddBelColumns... to menu
        registerService(bc,
            new AddBelColumnsToCurrentFactoryImpl(cyr),
            AddBelColumnsToCurrentFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 10.0,
                title: "Add Data Columns"
            ] as Properties)
        registerService(bc,
            new ExpandNodeFactory(cyr),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: "Expand Node"
            ] as Properties)
        registerService(bc,
            new LinkKnowledgeNetworkFactory(cyr),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 12.0,
                title: "Link to Knowledge Network"
            ] as Properties)
        registerService(bc,
                new KnowledgeNeighborhoodFactory(cyr, searchKnUI),
                NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 13.0,
                title: "Search Knowledge Neighborhood"
        ] as Properties)
        registerService(bc,
            new LoadFullKnowledgeNetworkFactory(cyr, cyProp),
            TaskFactory.class, [
                preferredMenu: 'File.New.Network',
                menuGravity: 14.0,
                title: 'From Knowledge Network'
            ] as Properties)

        // Search Nodes
        AbstractCyAction addNodesAction = new AbstractCyAction('Search Nodes') {
            void actionPerformed(ActionEvent e) {
                searchNodesUI.show(cyr.cySwingApplication, {
                    [
                        knowledgeNetworks: cyr.wsAPI.knowledgeNetworks().keySet().sort(),
                        functions: FunctionEnum.values().sort {it.displayValue} as List,
                        namespaces: cyr.wsAPI.getAllNamespaces().sort {it.name}
                    ]
                }, { kn, fx, ns, entities ->
                    entities = entities.collect {
                        if (it.endsWith('*')) {
                            it = it.length() == 1 ? '' : it[0..-2]
                            def nsValues = cyr.wsAPI.findNamespaceValues(
                                    [ns] as Collection<Namespace>,
                                    [~/${it}.*/] as Collection<Pattern>)
                            return nsValues.collect {it[1]}
                        }

                        return it
                    }.flatten().unique()
                    def cyN = cyr.cyApplicationManager.currentNetwork
                    def searchNodes = cyr.wsAPI.mapData(kn, ns,
                            [fx].findAll() as FunctionEnum[],
                            entities as String[]).collect {
                        [id: it.id, fx: it.fx, label: it.label, present: false]
                    }

                    if (cyN) {
                        def existingNodes = cyN.nodeList.collect {
                            cyN.getRow(it).get(CyNetwork.NAME, String.class)
                        } as Set
                        searchNodes.findAll {it.label in existingNodes}.each {it.present = true}
                    }

                    [searchNodes, "${searchNodes.size()} nodes"]
                }, { selectedNodes, connect ->
                    def cyN = cyr.cyApplicationManager.currentNetwork
                    def cyNv = cyr.cyApplicationManager.currentNetworkView
                    if (!cyN) return null

                    selectedNodes.each { n ->
                        CyNode cyNode = findNode(cyN, n.label) ?: makeNode(cyN, n.id, n.fx.displayValue, n.label)
                        if (connect) {
                            def node = toNode(cyN, cyNode)
                            cyr.wsAPI.adjacentEdges(node).each { edge ->
                                def s = edge.source
                                def t = edge.target
                                def rel = edge.relationship.displayValue

                                // only add adjacent edge if opposite node exists in network
                                def oppositeInNetwork = findNode(cyN, (n.id == s.id) ? t.label : s.label)
                                if (oppositeInNetwork) {
                                    def cySource = findNode(cyN, s.label)
                                    def cyTarget = findNode(cyN, t.label)
                                    findEdge(cyN, s.label, rel, t.label) ?:
                                        makeEdge(cyN, cySource, cyTarget, edge.id, rel)
                                }
                            }
                        }
                    }

                    // refresh network view
                    cyr.cyEventHelper.flushPayloadEvents()
                    cyr.visualMappingManager.getCurrentVisualStyle().apply(cyNv)
                    cyNv.updateView()

                    return selectedNodes.each {it.present = true}
                }, [:])
            }
        }
        addNodesAction.menuGravity = 0.0
        addNodesAction.preferredMenu = 'Apps.KamNav'
        addNodesAction.acceleratorKeyStroke = getKeyStroke('control alt N')
        registerService(bc, addNodesAction, CyAction.class, [
                id: 'apps_nav.search_nodes'
        ] as Properties)

        // initialization
        contributeVisualStyles(cyr.visualMappingManager, vf)
    }
}
