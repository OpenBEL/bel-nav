package org.openbel.ws.internal

import org.cytoscape.model.CyNetwork
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.framework.common.enums.RelationshipType
import org.openbel.framework.ws.model.FunctionType
import org.openbel.kamnav.common.model.Edge
import org.openbel.kamnav.common.model.Namespace
import org.openbel.kamnav.common.model.Node
import org.openbel.ws.api.WsAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.soap.SOAPClient
import wslite.soap.SOAPFaultException

import javax.net.ssl.SSLContext
import java.util.regex.Pattern

import static org.cytoscape.model.CyEdge.INTERACTION
import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.kamnav.common.util.EdgeUtil.createEdgeColumns
import static org.openbel.kamnav.common.util.NodeUtil.createNodeColumns
import static org.openbel.kamnav.common.util.NodeUtil.toBEL

/**
 * {@link WsAPI} implementation using groovy-wslite (soap xml builders).
 */
class BasicWsAPI implements WsAPI {

    static final String URL = 'https://selventa-sdp.selventa.com/openbel-ws/belframework'
    private static final Logger log = LoggerFactory.getLogger(BasicWsAPI.class)

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
            def response
            try {
                response = client.send {
                    body {
                        LoadKamRequest('xmlns': 'http://belframework.org/ws/schemas') {
                            kam {
                                mkp.yieldUnescaped "<name>${name}</name>"
                            }
                        }
                    }
                }
            } catch (SOAPFaultException ex) {
                soapError("LoadKamRequest", ex)
                throw ex
            }

            loadMap.status = response.LoadKamResponse.loadStatus
            while (loadMap.status == 'IN_PROCESS') {
                try {
                    response = client.send {
                        body {
                            LoadKamRequest('xmlns': 'http://belframework.org/ws/schemas') {
                                kam {
                                    mkp.yieldUnescaped "<name>${name}</name>"
                                }
                            }
                        }
                    }
                } catch (SOAPFaultException ex) {
                    soapError("LoadKamRequest", ex)
                    throw ex
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
        def response
        try {
            response = client.send {
                body {
                    GetCatalogRequest('xmlns': 'http://belframework.org/ws/schemas')
                }
            }
        } catch (SOAPFaultException ex) {
            soapError("GetCatalogRequest", ex)
            throw ex
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
        def response
        try {
            response = client.send {
                body {
                    GetAllNamespacesRequest('xmlns': 'http://belframework.org/ws/schemas')
                }
            }
        } catch (SOAPFaultException ex) {
            soapError("GetAllNamespacesRequest", ex)
            throw ex
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

        def response
        try {
            response = client.send {
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
        } catch (SOAPFaultException ex) {
            soapError("FindNamespaceValuesRequest", ex)
            throw ex
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

        def eligibleNodes = []

        def response
        try {
            response = client.send {
                body {
                    ResolveNodesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                        handle {
                            handle(loadMap.handle)
                        }
                        cyN.nodeList.collect { n ->
                            def bel = toBEL(cyN, n)
                            if (!bel) return null

                            // track what we're requesting; iterate this later
                            eligibleNodes << n

                            nodes {
                                function(toWS(bel.fx))
                                label(bel.lbl)
                            }
                        }
                    }
                }
            }
        } catch (SOAPFaultException ex) {
            soapError("ResolveNodesRequest", ex)
            throw ex
        }

        def resNodes = response.ResolveNodesResponse.kamNodes.iterator()
        if (!resNodes || !resNodes.hasNext()) [].asImmutable()

        eligibleNodes.collect { n ->
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

        def eligibleEdges = []

        def response
        try {
            response = client.send {
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

                            // track what we're requesting; iterate this later
                            eligibleEdges << e

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
        } catch (SOAPFaultException ex) {
            soapError("ResolveEdgesRequest", ex)
            throw ex
        }

        cyN.defaultEdgeTable.getColumn('kam.id') ?:
            cyN.defaultEdgeTable.createColumn('kam.id', String.class, false)

        def resEdges = response.ResolveEdgesResponse.kamEdges.iterator()
        if (!resEdges || !resEdges.hasNext()) [].asImmutable()

        eligibleEdges.collect { e ->
            if (!resEdges.hasNext()) return null

            def wsEdge = resEdges.next()
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

        def response
        try {
            response = client.send {
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
        } catch (SOAPFaultException ex) {
            soapError("FindKamNodesByPatternsRequest", ex)
            throw ex
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

        def response
        try {
            response = client.send {
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
        } catch (SOAPFaultException ex) {
            soapError("GetAdjacentKamEdgesRequest", ex)
            throw ex
        }

        response.GetAdjacentKamEdgesResponse.kamEdges.findAll {
            !it.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
        }.
        collect {
            new Edge(it.id.toString(),
                new Node(it.source.id.toString(),
                    FunctionEnum.fromString(FunctionType.valueOf(it.source.function.toString()).displayValue),
                    it.source.label.toString()),
                it.relationship.toString(),
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

        def response;
        try {
            response = client.send {
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
        } catch (SOAPFaultException ex) {
            soapError("MapDataRequest", ex)
            throw ex
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
        def response;
        try {
            response = client.send {
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
        } catch(SOAPFaultException ex) {
            soapError("GetSupportingEvidenceRequest", ex)
            throw ex
        }

        response.GetSupportingEvidenceResponse.statements.
        findAll {
            !it.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
        }.
        collect {
            [
                edge_source: edge.source.label,
                edge_rel: edge.relationship ?: '',
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
                citationType: it.citation.citationType.toString(),
                citationId: it.citation.id.toString(),
                citationName: it.citation.name.toString()
            ]
        }
    }

    private def fromWS(type) {
        if (!type) return null
        RelationshipType.values().find {it.name() == type}?.displayValue
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

    private def soapError(String endpoint, SOAPFaultException ex) {
        String msg = "Error calling ${endpoint}"
        if (ex.httpResponse) {
            def res = ex.httpResponse
            msg += " (code: ${res.statusCode}, msg: ${res.statusMessage}"
        }
        log.error(msg, ex)
    }
}
