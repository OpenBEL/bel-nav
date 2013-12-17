package org.openbel.ws.internal

import org.openbel.kamnav.common.model.Namespace

import javax.net.ssl.SSLContext

import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.model.CyEdge.INTERACTION
import static org.openbel.kamnav.common.util.EdgeUtil.createEdgeColumns
import static org.openbel.kamnav.common.util.NodeUtil.createNodeColumns
import static org.openbel.kamnav.common.util.NodeUtil.toBEL
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

    static final String URL = 'https://selventa-sdp.selventa.com/openbel-ws/belframework'

    BasicWsAPI() {
        SSLContext.default = SSL.context
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map loadKnowledgeNetwork(String name) {
        def client = new SOAPClient(this.URL)
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
        def client = new SOAPClient(this.URL)
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
    List<Namespace> getAllNamespaces() {
        def client = new SOAPClient(this.URL)
        def response = client.send {
            body {
                GetAllNamespacesRequest('xmlns': 'http://belframework.org/ws/schemas')
            }
        }

        response.GetAllNamespacesResponse.namespaceDescriptors.
        collect {
            new Namespace(
                it.namespace.id.toString(),
                it.name.toString(),
                it.namespace.prefix.toString(),
                it.namespace.resourceLocation.toString()
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List findNamespaceValues(Collection<Namespace> ns, Collection<Pattern> regex) {
        def client = new SOAPClient(this.URL)
        def response = client.send {
            body {
                FindNamespaceValuesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    ns.collect { nsItem ->
                        namespaces {
                            id(nsItem.id)
                            prefix(nsItem.prefix)
                            resourceLocation(nsItem.resourceLocation)
                        }
                    }
                    regex.collect {patterns(it.toString())}
                }
            }
        }

        response.FindNamespaceValuesResponse.namespaceValues.
        collect {
            [
                new Namespace(
                    it.namespace.id.toString(),
                    null,
                    it.namespace.prefix.toString(),
                    it.namespace.resourceLocation.toString()
                ),
                it.value
            ]
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List linkNodes(CyNetwork cyN, String name) {
        def client = new SOAPClient(this.URL)
        def loadMap = loadKnowledgeNetwork(name)

        if (!cyN.nodeList) [].asImmutable()

        createNodeColumns(cyN)
        cyN.nodeList.each {
            cyN.getRow(it).set('linked', false)
            cyN.getRow(it).set('kam.id', null)
        }

        def response = client.send {
            body {
                ResolveNodesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    handle {
                        handle(loadMap.handle)
                    }
                    cyN.nodeList.collect { n ->
                        def bel = toBEL(cyN, n)
                        if (!bel) return null
                        nodes {
                            function(toWS(bel.fx))
                            label(bel.lbl)
                        }
                    }
                }
            }
        }

        def resNodes = response.ResolveNodesResponse.kamNodes.iterator()
        if (!resNodes || !resNodes.hasNext()) [].asImmutable()

        cyN.nodeList.collect { n ->
            if (!toBEL(cyN, n)) return null
            if (!resNodes.hasNext()) return null
            def wsNode = resNodes.next()

            def isNil = wsNode.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
            if (isNil) return null

            def id = wsNode.id.toString()
            def fx = FunctionType.valueOf(wsNode.function.toString()).displayValue
            def lbl = wsNode.label.toString()

            cyN.getRow(n).set('linked', true)
            cyN.getRow(n).set("kam.id", id)
            cyN.getRow(n).set("bel.function", FunctionEnum.fromString(fx).displayValue)
            cyN.getRow(n).set(NAME, lbl)

            [id: id, fx: fx, lbl: lbl]
        }.asImmutable()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    List linkEdges(CyNetwork cyN, String name) {
        def client = new SOAPClient(this.URL)
        def loadMap = loadKnowledgeNetwork(name)
        if (!loadMap.handle) return null

        if(!cyN.edgeList) return [].asImmutable()

        createEdgeColumns(cyN)
        cyN.edgeList.each {
            cyN.getRow(it).set('linked', false)
            cyN.getRow(it).set('kam.id', null)
        }

        def response = client.send {
            body {
                ResolveEdgesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    handle {
                        handle(loadMap.handle)
                    }
                    cyN.edgeList.collect { e ->
                        def src = toBEL(cyN, e.source)
                        def tgt = toBEL(cyN, e.target)
                        def r = cyN.getRow(e).get(INTERACTION, String.class)
                        if (!r || !toWS(r)) return null
                        if (!src || !tgt) return null

                        edges {
                            source {
                                function(toWS(src.fx))
                                label(src.lbl)
                            }
                            relationship(toWS(r))
                            target {
                                function(toWS(tgt.fx))
                                label(tgt.lbl)
                            }
                        }
                    }
                }
            }
        }

        cyN.defaultEdgeTable.getColumn('kam.id') ?:
            cyN.defaultEdgeTable.createColumn('kam.id', String.class, false)

        def resEdges = response.ResolveEdgesResponse.kamEdges.iterator()
        if (!resEdges || !resEdges.hasNext()) [].asImmutable()

        cyN.edgeList.collect { e ->
            if (!resEdges.hasNext()) return null
            def wsEdge = resEdges.next()

            def src = toBEL(cyN, e.source)
            def tgt = toBEL(cyN, e.target)
            if (!src || !tgt) return null
            def isNil = wsEdge.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
            if (isNil) return null

            cyN.getRow(e).set('linked', true)
            cyN.getRow(e).set("kam.id", wsEdge.id.toString())
            [
                id: wsEdge.id.toString(),
                source: [
                    fx: FunctionEnum.fromString(FunctionType.valueOf(wsEdge.source.function.toString()).displayValue),
                    lbl: wsEdge.source.label.toString()
                ],
                relationship: RelationshipType.fromString(org.openbel.framework.ws.model.RelationshipType.valueOf(wsEdge.relationship.toString()).displayValue),
                target: [
                    fx: FunctionEnum.fromString(FunctionType.valueOf(wsEdge.target.function.toString()).displayValue),
                    lbl: wsEdge.target.label.toString()
                ]
            ]
        }.asImmutable()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Node[] findNodes(String name, Pattern labelPattern, FunctionEnum... functions) {
        def client = new SOAPClient(this.URL)
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
        def client = new SOAPClient(this.URL)

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
    List<Node> mapData(String name, Namespace ns, FunctionEnum[] functions, String[] entities) {
        def client = new SOAPClient(this.URL)
        def loadMap = loadKnowledgeNetwork(name)
        if (!loadMap.handle) return null

        def response = client.send {
            body {
                MapDataRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    handle {
                        handle(loadMap.handle)
                    }
                    namespace {
                        id(ns.id)
                        prefix(ns.prefix)
                        resourceLocation(ns.resourceLocation)
                    }
                    if (functions) {
                        nodeFilter {
                            functionTypeCriteria {
                                functions.collect {
                                    valueSet(toWS(it))
                                }
                            }
                        }
                    }
                    entities.collect {values(it)}
                }
            }
        }

        response.MapDataResponse.kamNodes.
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
    List<Map> getSupportingEvidence(Edge edge) {
        def client = new SOAPClient(this.URL)
        def response = client.send {
            body {
                GetSupportingEvidenceRequest('xmlns': 'http://belframework.org/ws/schemas') {
                    kamEdge {
                        id(edge.id)
                        source(edge.source)
                        relationship(edge.relationship)
                        target(edge.target)
                    }
                }
            }
        }

        response.GetSupportingEvidenceResponse.statements.
        findAll {
            !it.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
        }.
        collect {
            [
                edge_source: edge.source.label,
                edge_rel: edge.relationship.displayValue,
                edge_target: edge.target.label,
                subject: it.subjectTerm.label.toString(),
                relationship: fromWS(it.relationship.toString()),
                objectTerm: it.objectTerm?.label?.toString(),
                nestedSubject: it.objectStatement?.subjectTerm?.label?.toString(),
                nestedRelationship: fromWS(it.objectStatement?.relationship?.toString()),
                nestedObject: it.objectStatement?.objectTerm?.label?.toString(),
                annotations: it.annotations.iterator().collectEntries { anno ->
                   [anno.annotationType.name.toString(), anno.value.toString()]
                },
                citation: it.citation.name.toString()
            ]
        }
    }

    private def fromWS(type) {
        switch (type) {
            case org.openbel.framework.ws.model.RelationshipType:
                RelationshipType.fromString(type.displayValue)
                break
            default:
                type
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
}
