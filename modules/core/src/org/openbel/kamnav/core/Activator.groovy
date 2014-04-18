package org.openbel.kamnav.core

import org.cytoscape.application.CyApplicationManager
import org.cytoscape.application.swing.AbstractCyAction
import org.cytoscape.application.swing.CyAction
import org.cytoscape.application.swing.CySwingApplication
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.*
import org.cytoscape.property.CyProperty
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.session.events.SessionLoadedListener
import org.cytoscape.task.EdgeViewTaskFactory
import org.cytoscape.task.NetworkViewTaskFactory
import org.cytoscape.task.NodeViewTaskFactory
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskManager
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.kamnav.common.model.Namespace
import org.openbel.kamnav.core.event.BELNetworkListener
import org.openbel.kamnav.core.event.SessionLoadListener
import org.openbel.kamnav.core.task.*
import org.openbel.kamnav.ui.ConfigurationUI
import org.openbel.kamnav.ui.EdgeUpdateable
import org.openbel.kamnav.ui.SearchNeighborhoodUI
import org.openbel.kamnav.ui.SearchNodesDialogUI
import org.openbel.ws.api.WsManager
import org.osgi.framework.BundleContext

import java.awt.event.ActionEvent
import java.util.regex.Pattern

import static javax.swing.KeyStroke.getKeyStroke
import static org.openbel.kamnav.common.Constant.setLoggingExceptionHandler
import static org.openbel.kamnav.common.util.EdgeUtil.findEdge
import static org.openbel.kamnav.common.util.EdgeUtil.makeEdge
import static org.openbel.kamnav.common.util.NodeUtil.*
import static org.openbel.kamnav.common.util.Util.cyReference
import static org.openbel.kamnav.core.Util.contributeVisualStyles
import static org.openbel.kamnav.core.Util.getCurrentNetwork

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        def cyr = cyReference(bc, this.&getService, [CyApplicationManager.class,
                CySwingApplication.class, CyNetworkFactory.class, CyNetworkManager.class,
                CyNetworkViewFactory.class, CyNetworkViewManager.class,
                CyLayoutAlgorithmManager.class, CyTableFactory.class, CyTableManager.class,
                VisualMappingManager.class, CyEventHelper.class,
                ApplyPreferredLayoutTaskFactory.class, TaskManager.class, WsManager.class] as Class<?>[])
        VisualMappingFunctionFactory vmFxFactory = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
        CyProperty<Properties> cyProp = getService(bc,CyProperty.class,"(cyPropertyName=cytoscape3.props)");
        ConfigurationUI configUI = getService(bc, ConfigurationUI.class)
        SearchNodesDialogUI searchNodesUI = getService(bc, SearchNodesDialogUI.class)
        SearchNeighborhoodUI searchKnUI = getService(bc, SearchNeighborhoodUI.class)

        def evUpdateable = getService(bc, EdgeUpdateable.class, "(name=evidence)")

        // register listeners
        LoadVizmapFileTaskFactory vf =  getService(bc,LoadVizmapFileTaskFactory.class)
        registerService(bc,
            new SessionLoadListener(cyr.visualMappingManager, vf),
            SessionLoadedListener.class, [:] as Properties)
        registerAllServices(bc, new BELNetworkListener(cyr), [:] as Properties)

        // register tasks
        registerService(bc,
            new AddBelColumnsToCurrentFactoryImpl(cyr),
            AddBelColumnsToCurrentFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 10.0,
                title: 'Add Data Columns'
            ] as Properties)
        registerService(bc,
            new ExpandNodeFactory(cyr),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 11.0,
                title: 'Expand Node'
            ] as Properties)
        registerService(bc,
            new LinkKnowledgeNetworkFactory(cyr),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 12.0,
                accelerator: 'control alt L',
                title: 'Link to Knowledge Network'
            ] as Properties)
        registerService(bc,
            new KnowledgeNeighborhoodFactory(cyr, searchKnUI),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 13.0,
                title: 'Show Neighborhood'
            ] as Properties)
        registerService(bc,
            new ShowEvidenceFactory(cyr, evUpdateable),
            EdgeViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 14.0,
                title: 'Show Evidence'
            ] as Properties)
        registerService(bc,
            new LoadFullKnowledgeNetworkFactory(cyr, cyProp),
            TaskFactory.class, [
                preferredMenu: 'File.New.Network',
                menuGravity: 15.0,
                title: 'From Knowledge Network'
            ] as Properties)
        registerService(bc,
            new ViewAsEntityFactory(cyr.visualMappingManager, vmFxFactory),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.KamNav',
                menuGravity: 16.0,
                title: 'View Network as Entities'
            ] as Properties)

        // Configuration
        AbstractCyAction configAction = new AbstractCyAction('Configure') {
            @Override
            void actionPerformed(ActionEvent e) {
                def mgr = cyr.wsManager
                configUI.configurationDialog(mgr, {url -> true}, { items ->
                    mgr.removeAll()

                    items.each { item ->
                        mgr.add(item.url)
                    }
                    mgr.default = items.find { it.default }.url
                    cyr.wsManager.saveConfiguration()
                })
            }
        }
        configAction.menuGravity = 0.0
        configAction.preferredMenu = 'Apps.KamNav'
        registerService(bc, configAction, CyAction.class, [
                id: 'apps_nav.config'
        ] as Properties)

        // Search Nodes
        AbstractCyAction addNodesAction = new AbstractCyAction('Search Nodes') {
            void actionPerformed(ActionEvent e) {
                def cyN = cyr.cyApplicationManager.currentNetwork
                def cyNv = cyr.cyApplicationManager.currentNetworkView

                def wsAPI = cyr.wsManager.get(cyr.wsManager.default)
                searchNodesUI.show(cyr.cySwingApplication, {
                    [
                        knowledgeNetworks: wsAPI.knowledgeNetworks().keySet().sort(),
                        functions: FunctionEnum.values().sort {it.displayValue} as List,
                        namespaces: wsAPI.getAllNamespaces().sort {it.name}
                    ]
                }, { kn, fx, ns, entities ->
                    entities = entities.collect {
                        if (it.endsWith('*')) {
                            it = it.length() == 1 ? '' : it[0..-2]
                            def nsValues = wsAPI.findNamespaceValues(
                                    [ns] as Collection<Namespace>,
                                    [~/${it}.*/] as Collection<Pattern>)
                            return nsValues.collect {it[1]}
                        }

                        return it
                    }.flatten().unique()
                    def searchNodes = wsAPI.mapData(kn, ns,
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
                    (cyN, cyNv) = getCurrentNetwork(cyr, true)

                    selectedNodes.each { n ->
                        CyNode cyNode = findNode(cyN, n.label) ?: makeNode(cyN, n.id, n.fx.displayValue, n.label)
                        if (connect) {
                            def node = toNode(cyN, cyNode)
                            wsAPI.adjacentEdges(node).each { edge ->
                                def s = edge.source
                                def t = edge.target
                                def rel = edge.relationship

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

        setLoggingExceptionHandler()
    }
}
