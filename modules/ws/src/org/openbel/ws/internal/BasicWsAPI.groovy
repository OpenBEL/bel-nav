package org.openbel.ws.internal

import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.kamnav.common.util.NodeUtil.props
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.cytoscape.model.CyNetwork
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.framework.common.enums.RelationshipType
import org.openbel.framework.ws.model.FunctionType
import org.openbel.kamnav.common.model.Node
import org.openbel.kamnav.common.model.Edge
import org.openbel.ws.api.WsAPI
import wslite.soap.*

import java.util.regex.Pattern

/**
 * {@link WsAPI} implementation using groovy-wslite (soap xml builders).
 */
class BasicWsAPI implements WsAPI {

    private static Logger log = LoggerFactory.getLogger(getClass())

    /**
     * {@inheritDoc}
     */
    @Override
    Map loadKnowledgeNetwork(String name) {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def loadMap = [name: name]

        Thread load = Thread.start {
            def response = client.send {
                body {
                    LoadKamRequest('xmlns': 'http://belframework.org/ws/schemas') {
                        kam {
                            mkp.yieldUnescaped "<name>${name}</name>"
                        }
                    }
                }
            }
            loadMap.status = response.LoadKamResponse.loadStatus
            while (loadMap.status == 'IN_PROCESS') {
                response = client.send {
                    body {
                        LoadKamRequest('xmlns': 'http://belframework.org/ws/schemas') {
                            kam {
                                mkp.yieldUnescaped "<name>${name}</name>"
                            }
                        }
                    }
                }
                loadMap.status = response.LoadKamResponse.loadStatus
                sleep(1000)
            }

            if (loadMap.status == 'FAILED') {
                loadMap.message = response.LoadKamResponse.message
            } else if (loadMap.status == 'COMPLETE') {
                loadMap.handle = response.LoadKamResponse.handle
                loadMap.message = "${name} was loaded successfully."
            } else if (loadMap.status == 'IN_PROCESS') {
                loadMap.message = "${name} is in the process of loading."
            }
        }

        load.join()
        loadMap
    }

    /**
     * {@inheritDoc}
     */
    @Override Map knowledgeNetworks() {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def response = client.send {
            body {
                GetCatalogRequest('xmlns': 'http://belframework.org/ws/schemas')
            }
        }

        response.GetCatalogResponse.kams.
            collect {[
                id: it.id.toString(),
                name: it.name.toString(),
                description: it.description.toString(),
                lastCompiled: it.lastCompiled.toString()
            ]}.
            groupBy { it.name }.
            collectEntries { k, v -> [(k): v.first()]}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void link(CyNetwork cyn, String name, Closure closure = null) {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def loadMap = loadKnowledgeNetwork(name)

        def response = client.send {
            body {
                ResolveNodesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    handle {
                        handle(loadMap.handle)
                    }
                    cyn.nodeList.collect { n ->
                        def (fx, lbl) = props.call(cyn, n)
                        if (fx && lbl) {
                            nodes {
                                function(toWS(fx))
                                label(lbl)
                            }
                        }
                    }
                }
            }
        }

        response.ResolveNodesResponse.kamNodes.eachWithIndex { n, idx ->
            def isNil = n.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
            def cyNode = cyn.nodeList[idx]
            if (!isNil) {
                def id = n.id.toString()
                def fx = FunctionType.valueOf(n.function.toString()).displayValue
                def lbl = n.label.toString()

                if (!cyn.defaultNodeTable.getColumn('bel.function'))
                    cyn.defaultNodeTable.createColumn('bel.function', String.class, false)
                if (!cyn.defaultNodeTable.getColumn('kam.id'))
                    cyn.defaultNodeTable.createColumn('kam.id', String.class, false)

                cyn.getRow(cyNode).set(NAME, lbl)
                cyn.getRow(cyNode).set("bel.function",
                        FunctionEnum.fromString(fx).displayValue)
                cyn.getRow(cyNode).set("kam.id", id)

                if (closure) closure.call(cyNode, [id: id, fx: fx, lbl: lbl])
            } else {
                if (closure) closure.call(cyNode, [:])
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Node[] findNodes(String name, Pattern labelPattern, FunctionEnum... functions) {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def loadMap = loadKnowledgeNetwork(name)
        if (!loadMap.handle) return null

        def response = client.send {
            body {
                FindKamNodesByPatternsRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    handle {
                        handle(loadMap.handle)
                    }
                    patterns(labelPattern.toString())
                    filter {
                        functionTypeCriteria {
                            functions.collect {
                                valueSet(toWS(it))
                            }
                        }
                    }
                }
            }
        }

        response.FindKamNodesByPatternsResponse.kamNodes.
                findAll {
                    !it.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
                }.
                collect {
                    String id = it.id.toString()
                    String fx = FunctionType.valueOf(it.function.toString()).displayValue
                    String label = it.label.toString()
                    new Node(id, FunctionEnum.fromString(fx), label)
                }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Edge[] adjacentEdges(Node node, String dir = 'BOTH') {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')

        def response = client.send {
            body {
                GetAdjacentKamEdgesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    kamNode {
                        id(node.id)
                        function(toWS(node.fx))
                        label(node.label)
                    }
                    direction(dir)
                }
            }
        }

        response.GetAdjacentKamEdgesResponse.kamEdges.findAll {
            !it.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
        }.
        collect {
            new Edge(it.id.toString(),
                new Node(it.source.id.toString(),
                    FunctionEnum.fromString(FunctionType.valueOf(it.source.function.toString()).displayValue),
                    it.source.label.toString()),
                RelationshipType.fromString(org.openbel.framework.ws.model.RelationshipType.valueOf(it.relationship.toString()).displayValue),
                new Node(it.target.id.toString(),
                    FunctionEnum.fromString(FunctionType.valueOf(it.target.function.toString()).displayValue),
                    it.target.label.toString()))
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Node[] resolveNodes(String name, Node[] nodelist) {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def loadMap = loadKnowledgeNetwork(name)
        if (!loadMap.handle) return null

        def response = client.send {
            body {
                ResolveNodesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    handle {
                        handle(loadMap.handle)
                    }
                    nodelist.collect { n ->
                        nodes {
                            function(toWS(n.fx))
                            label(n.label)
                        }
                    }
                }
            }
        }

        response.ResolveNodesResponse.kamNodes.
        findAll {
            !it.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
        }.
        collect {
            String id = it.id.toString()
            String fx = FunctionType.valueOf(it.function.toString()).displayValue
            String label = it.label.toString()
            new Node(id, FunctionEnum.fromString(fx), label)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Edge[] resolveEdges(String name, Edge[] edgelist) {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def loadMap = loadKnowledgeNetwork(name)
        if (!loadMap.handle) return null

        def response = client.send {
            body {
                ResolveEdgesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    handle {
                        handle(loadMap.handle)
                    }
                    edgelist.collect { e ->
                        edges {
                            source {
                                function(toWS(e.source.fx))
                                label(e.source.label)
                            }
                            relationship(toWS(e.relationship))
                            target {
                                function(toWS(e.target.fx))
                                label(e.target.label)
                            }
                        }
                    }
                }
            }
        }

        response.ResolveEdgesResponse.kamEdges.findAll {
            !it.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
        }.
        collect {
            Node source = new Node(
                    it.source.id.toString(),
                    FunctionEnum.fromString(FunctionType.valueOf(it.source.function.toString()).displayValue),
                    it.source.label.toString())
            def rel = RelationshipType.fromString(org.openbel.framework.ws.model.RelationshipType.valueOf(it.relationship.toString()).displayValue)
            Node target = new Node(
                    it.target.id.toString(),
                    FunctionEnum.fromString(FunctionType.valueOf(it.target.function.toString()).displayValue),
                    it.target.label.toString())
            new Edge(null, source, rel, target)
        }
    }

    private def toWS(type) {
        switch (type) {
            case FunctionEnum:
                FunctionType.fromValue(type.displayValue)
                break;
            case RelationshipType:
                org.openbel.framework.ws.model.RelationshipType.fromValue(type.displayValue)
                break;
            case String:
                FunctionEnum fx = FunctionEnum.fromString(type)
                if (fx)
                    FunctionType.fromValue(fx.displayValue)
                else {
                    RelationshipType r = [
                        RelationshipType.fromString(type),
                        RelationshipType.fromAbbreviation(type)
                    ].find()
                    if (r)
                        org.openbel.framework.ws.model.RelationshipType.fromValue(r.displayValue)
                }
                break;
        }
    }

    static void main(args) {
        def api = new BasicWsAPI()
        println api.toWS(FunctionEnum.ABUNDANCE)
        println api.toWS(RelationshipType.CAUSES_NO_CHANGE)
        println api.toWS("transcriptionalActivity")
        println api.toWS("tscript")
        println api.toWS("=|")
        println api.toWS("directlyDecreases")

        println "GetCatalog...Hash keyed by knowledge network name"
        def kams = api.knowledgeNetworks()
        println "returned, map keys: ${kams.keySet()}, map: $kams"
        println()

        if (kams) {
            String name = kams.take(1).values().first().name
            println "LoadKam...Grab first one ($name)"
            println api.loadKnowledgeNetwork(name)

            Node source = new Node(null, FunctionEnum.KINASE_ACTIVITY, 'kin(p(HGNC:AKT1))')
            Node target = new Node(null, FunctionEnum.PROTEIN_ABUNDANCE, 'p(HGNC:CDKN1A)')
            Edge edge = new Edge(null, source, RelationshipType.CAUSES_NO_CHANGE, target)
            println api.resolveNodes(name, [source, target] as Node[])
            println api.resolveEdges(name, [edge] as Edge[])
        }
        println "LoadKam...Does not exist"
        println api.loadKnowledgeNetwork('Does not exist')
    }
}
