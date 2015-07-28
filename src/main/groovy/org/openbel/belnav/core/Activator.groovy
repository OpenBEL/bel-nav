package org.openbel.belnav.core

import groovy.swing.SwingBuilder
import groovy.transform.PackageScope
import org.cytoscape.application.CyApplicationConfiguration
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
import org.cytoscape.view.vizmap.VisualStyleFactory
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskManager
import org.jdesktop.swingx.JXHyperlink
import org.jdesktop.swingx.JXList
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.JXTaskPane
import org.jdesktop.swingx.JXTaskPaneContainer
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.belnav.common.model.Namespace
import org.openbel.belnav.core.event.BELNetworkListener
import org.openbel.belnav.core.event.SessionLoadListener
import org.openbel.belnav.core.task.*
import org.openbel.belnav.ui.ConfigurationUI
import org.openbel.belnav.ui.ConfigurationUIImpl
import org.openbel.belnav.ui.EdgeUpdateable
import org.openbel.belnav.ui.EvidencePanel
import org.openbel.belnav.ui.EvidencePanelComponent
import org.openbel.belnav.ui.SearchNeighborhoodUI
import org.openbel.belnav.ui.SearchNeighborhoodUIImpl
import org.openbel.belnav.ui.SearchNodesDialogUI
import org.openbel.belnav.ui.SearchNodesDialogUIImpl
import org.openbel.ws.api.WsAPI
import org.openbel.ws.api.WsManager
import org.openbel.ws.internal.BasicWsManager
import org.osgi.framework.BundleContext

import java.awt.event.ActionEvent
import java.util.regex.Pattern

import static javax.swing.KeyStroke.getKeyStroke
import static org.openbel.belnav.common.Constant.setLoggingExceptionHandler
import static org.openbel.belnav.common.util.EdgeUtil.findEdge
import static org.openbel.belnav.common.util.EdgeUtil.makeEdge
import static org.openbel.belnav.common.util.NodeUtil.*
import static org.openbel.belnav.common.util.Util.cyReference
import static org.openbel.belnav.core.Util.contributeVisualStyles
import static org.openbel.belnav.core.Util.getCurrentNetwork

class Activator extends AbstractCyActivator {

    static Activator act;
    private static BundleContext ctx = null;

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        ctx = bc
        act = this

        def cyr = cyReference(
                bc, this.&getService,
                [
                    CyApplicationManager.class, CySwingApplication.class, CyNetworkFactory.class,
                    CyNetworkManager.class, CyNetworkViewFactory.class, CyNetworkViewManager.class,
                    CyLayoutAlgorithmManager.class, CyTableFactory.class, CyTableManager.class,
                    VisualMappingManager.class, CyEventHelper.class, VisualStyleFactory.class,
                    ApplyPreferredLayoutTaskFactory.class, TaskManager.class
                ] as Class<?>[]
        )

        // Ws
        WsManager wsManager = setupWsManager(bc)
        registerService(bc, wsManager, WsManager.class, [:] as Properties)
        cyr.wsManager = wsManager

        // UI
        def swing = new SwingBuilder()
        swing.registerBeanFactory('taskPaneContainer', JXTaskPaneContainer.class)
        swing.registerBeanFactory('taskPane', JXTaskPane.class)
        swing.registerBeanFactory('jxList', JXList.class)
        swing.registerBeanFactory('jxTable', JXTable.class)
        swing.registerBeanFactory('jxHyperlink', JXHyperlink.class)

        ConfigurationUI configUI = new ConfigurationUIImpl()
        SearchNodesDialogUI searchNodesUI = new SearchNodesDialogUIImpl(swing)
        SearchNeighborhoodUI searchKnUI = new SearchNeighborhoodUIImpl(swing)

        registerAllServices(bc, new EvidencePanelComponent(cyr,
                new EvidencePanel(swing, cyr)), [name: 'evidence'] as Properties)

        // Core
        CyProperty<Properties> cyProp = getService(bc,CyProperty.class,"(cyPropertyName=cytoscape3.props)");
        VisualMappingFunctionFactory dMapFac = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

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
                preferredMenu: 'Apps.BEL Navigator',
                menuGravity: 10.0,
                title: 'Add Data Columns'
            ] as Properties)
        registerService(bc,
            new ExpandNodeFactory(cyr),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.BEL Navigator',
                menuGravity: 11.0,
                title: 'Expand Node'
            ] as Properties)
        registerService(bc,
            new LinkKnowledgeNetworkFactory(cyr),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.BEL Navigator',
                menuGravity: 12.0,
                accelerator: 'control alt L',
                title: 'Link to Knowledge Network'
            ] as Properties)
        registerService(bc,
            new KnowledgeNeighborhoodFactory(cyr, searchKnUI),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.BEL Navigator',
                menuGravity: 13.0,
                title: 'Show Neighborhood'
            ] as Properties)
        registerService(bc,
            new ShowEvidenceFactory(cyr, evUpdateable),
            EdgeViewTaskFactory.class, [
                preferredMenu: 'Apps.BEL Navigator',
                menuGravity: 14.0,
                title: 'Show Evidence'
            ] as Properties)
        registerService(bc,
            new ValidateBELNetworkFactory(cyr.cyEventHelper, cyr.visualStyleFactory, cyr.visualMappingManager, dMapFac),
            NetworkViewTaskFactory.class, [
                preferredMenu: 'Apps.BEL Navigator',
                menuGravity: 15.0,
                accelerator: 'control alt V',
                title: 'Validate'
            ] as Properties)
        registerService(bc,
            new LoadFullKnowledgeNetworkFactory(cyr, cyProp),
            TaskFactory.class, [
                preferredMenu: 'File.New.Network',
                menuGravity: 15.0,
                title: 'From Knowledge Network'
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
        configAction.preferredMenu = 'Apps.BEL Navigator'
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
                    def nsSelection = (ns == 'All') ? null : [ns] as Collection<Namespace>
                    def mapToNamespaces = [:].withDefault{ [] as Set }
                    entities.collect {
                        it = it.replace('*', '.*')
                        def nsValues = wsAPI.findNamespaceValues(
                                nsSelection,
                                [~/(?i)${it}/] as Collection<Pattern>)
                        nsValues.each {
                            def res = new Namespace(null, null, null, it[0].resourceLocation)
                            mapToNamespaces[res] << it[1]
                        }
                    }
                    def searchNodes = [] as Set

                    mapToNamespaces.each { namespace, values ->
                        wsAPI.mapData(kn, namespace,
                                [fx].findAll() as FunctionEnum[],
                                values as String[]).collect {
                            searchNodes << [id: it.id, fx: it.fx, label: it.label, present: false]
                        }
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
        addNodesAction.preferredMenu = 'Apps.BEL Navigator'
        addNodesAction.acceleratorKeyStroke = getKeyStroke('control alt N')
        registerService(bc, addNodesAction, CyAction.class, [
                id: 'apps_nav.search_nodes'
        ] as Properties)

        // initialization
        contributeVisualStyles(cyr.visualMappingManager, vf)

        setLoggingExceptionHandler()
    }

    @PackageScope
    void register(WsAPI wsAPI) {
        if (ctx == null) throw new IllegalStateException("ctx is null");
        registerAllServices(ctx, wsAPI, ['uri': wsAPI.serviceLocation.toString()] as Properties)
    }

    private WsManager setupWsManager(BundleContext bc) {
        CyApplicationConfiguration cyAppConfig = getService(bc, CyApplicationConfiguration.class)
        File configDir = cyAppConfig.getAppConfigurationDirectoryLocation(Activator.class)
        new BasicWsManager(configDir)
    }
}
