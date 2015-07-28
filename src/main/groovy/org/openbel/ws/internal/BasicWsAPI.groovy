package org.openbel.ws.internal

import org.cytoscape.model.CyNetwork
import org.cytoscape.work.TaskIterator
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.framework.common.enums.RelationshipType
import org.openbel.framework.ws.model.FunctionType
import org.openbel.belnav.common.model.Edge
import org.openbel.belnav.common.model.Namespace
import org.openbel.belnav.common.model.Node
import org.openbel.ws.api.WsAPI
import wslite.http.HTTPClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.soap.SOAPClient
import wslite.soap.SOAPFaultException

import javax.net.ssl.SSLContext
import java.util.regex.Pattern

import static org.cytoscape.model.CyEdge.INTERACTION
import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.belnav.common.util.EdgeUtil.computeEdgeLabel
import static org.openbel.belnav.common.util.EdgeUtil.createEdgeColumns
import static org.openbel.belnav.common.util.NodeUtil.createNodeColumns
import static org.openbel.belnav.common.util.NodeUtil.toBEL

/**
 * {@link WsAPI} implementation using groovy-wslite (soap xml builders).
 */
class BasicWsAPI implements WsAPI {

    private static final String WS_NAME = "OpenBEL Web API"
    private static final String WS_DESC = "Web API to the OpenBEL server."
    private static final Logger log = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

    private final SOAPClient client
    private final URI location

    BasicWsAPI(URI location) {
        SSLContext.default = SSL.context
        this.location = location
        client = new SOAPClient(
                location.toString(),
                new HTTPClient(new ConfigurableConnectionFactory()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map loadKnowledgeNetwork(String name) {
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
        def loadMap = loadKnowledgeNetwork(name)

        if (!cyN.nodeList) [].asImmutable()

        createNodeColumns(cyN)
        cyN.nodeList.each {
            cyN.getRow(it).set('linked', false)
            cyN.getRow(it).set('kam.id', null)
        }

        def validCyNodes = cyN.nodeList.
                findAll { n ->
                    def bel = toBEL(cyN, n)
                    if (!bel) return false
                    def wsFx = toWS(bel.fx)
                    if (!wsFx) return false
                    true
                }

        def validBELNodes = validCyNodes.
                collect { n ->
                    def bel = toBEL(cyN, n)
                    if (!bel) return null
                    def wsFx = toWS(bel.fx)
                    if (!wsFx) return null
                    bel
                }.
                findAll()
        if (!validBELNodes) return [].asImmutable()

        def response
        try {
            response = client.send {
                body {
                    ResolveNodesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                        handle {
                            handle(loadMap.handle)
                        }
                        validBELNodes.collect { bel ->
                            nodes {
                                function(bel.fx)
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

        validCyNodes.collect { n ->
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
        def loadMap = loadKnowledgeNetwork(name)
        if (!loadMap.handle) return null

        if(!cyN.edgeList) return [].asImmutable()
        createEdgeColumns(cyN)
        cyN.edgeList.each {
            cyN.getRow(it).set('linked', false)
            cyN.getRow(it).set('kam.id', null)
        }

        def validBELEdges = cyN.edgeList.collect { e ->
            def src = toBEL(cyN, e.source)
            def tgt = toBEL(cyN, e.target)
            def r   = cyN.getRow(e).get(INTERACTION, String.class)
            def wsR = toWS(r)
            if (!src || !tgt || !r || !wsR) return null

            def sourceWsFx = toWS(src.fx)
            if (!sourceWsFx) return null
            src.fx = sourceWsFx
            def targetWsFx = toWS(tgt.fx)
            if (!targetWsFx) return null
            tgt.fx = targetWsFx

            [ source: src, relationship: wsR, target: tgt, cyE: e]
        }.findAll()

        if (!validBELEdges) return [].asImmutable()

        def response
        try {
            response = client.send {
                body {
                    ResolveEdgesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                        handle {
                            handle(loadMap.handle)
                        }
                        validBELEdges.collect { edge ->
                            edges {
                                source {
                                    function(edge.source.fx)
                                    label(edge.source.lbl)
                                }
                                relationship(edge.relationship)
                                target {
                                    function(edge.target.fx)
                                    label(edge.target.lbl)
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

        validBELEdges.collect { edge ->
            if (!resEdges.hasNext()) return null

            def wsEdge = resEdges.next()
            def isNil = wsEdge.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
            if (isNil) return null

            def cyE       = edge.cyE
            def edgeLabel = computeEdgeLabel(cyN, cyE)
            cyN.getRow(cyE).set(NAME, edgeLabel)
            cyN.getRow(cyE).set("shared name", edgeLabel)
            cyN.getRow(cyE).set('linked', true)
            cyN.getRow(cyE).set("kam.id", wsEdge.id.toString())
            [
                id: wsEdge.id.toString(),
                source: [
                    fx: FunctionEnum.fromString(FunctionType.valueOf(wsEdge.source.function.toString()).displayValue),
                    lbl: wsEdge.source.label.toString()
                ],
                relationship: fromWS(wsEdge.relationship.toString()),
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
        def loadMap = loadKnowledgeNetwork(name)
        if (!loadMap.handle) return null

        def validFunctions = functions.
                collect { fx ->
                    toWS(fx)
                }.
                findAll()

        def response
        try {
            response = client.send {
                body {
                    FindKamNodesByPatternsRequest('xmlns': 'http://belframework.org/ws/schemas') {
                        handle {
                            handle(loadMap.handle)
                        }
                        patterns(labelPattern.toString())
                        if (functions) {
                            filter {
                                functionTypeCriteria {
                                    validFunctions.collect { wsFx ->
                                        valueSet(wsFx)
                                    }
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
    Edge[] adjacentEdges(Node node, String dir = 'BOTH') {
        def response

        def nodeWsFx = toWS(node.fx)
        if (!nodeWsFx) {
            return new Edge[0]
        }

        try {
            response = client.send {
                body {
                    GetAdjacentKamEdgesRequest('xmlns': 'http://belframework.org/ws/schemas') {
                        kamNode {
                            id(node.id)
                            function(nodeWsFx)
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
                fromWS(it.relationship.toString()),
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
        def loadMap = loadKnowledgeNetwork(name)
        if (!loadMap.handle) return null

        if (entities?.length == 0) return null

        def validFunctions = functions.
                collect { fx ->
                    toWS(fx)
                }.
                findAll()

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
                                    validFunctions.collect { wsFx ->
                                        valueSet(wsFx)
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
        def response;
        try {
            response = client.send {
                body {
                    GetSupportingEvidenceMultipleRequest('xmlns': 'http://belframework.org/ws/schemas') {
                        kamEdges {
                            id(edge.id)
                            source(edge.source)
                            relationship(edge.relationship)
                            target(edge.target)
                        }
                    }
                }
            }
        } catch(SOAPFaultException ex) {
            soapError("GetSupportingEvidenceMultipleRequest", ex)
            throw ex
        }

        response.GetSupportingEvidenceMultipleResponse.edgeStatements.
        findAll {
            !it.attributes()['{http://www.w3.org/2001/XMLSchema-instance}nil']
        }.
        collect {
            def outer = it.statement
            [
                statement: outer.statement.toString(),
                annotations: outer.annotations.iterator().collectEntries { anno ->
                    [anno.annotationType.name.toString(), anno.value.toString()]
                },
                citationType: outer.citation.citationType.toString(),
                citationId: outer.citation.id.toString(),
                citationName: outer.citation.name.toString(),
                citationComment: outer.citation.comment?.toString() ?: '',
                citationDate: outer.citation.publicationDate?.toString() ?: ''
            ]
        }
    }

    private static def fromWS(type) {
        if (!type) return null
        RelationshipType.values().find {it.name() == type}?.displayValue
    }

    private static def toWS(type) {
        try {
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
        } catch (IllegalArgumentException e) {
            // the type is invalid within the ws.model enums; Enum.valueOf(...) will throw IllegalArgumentException
            return null;
        }
    }

    private static def soapError(String endpoint, SOAPFaultException ex) {
        String msg = "Error calling ${endpoint}"
        if (ex.httpResponse) {
            def res = ex.httpResponse
            msg += " (code: ${res.statusCode}, msg: ${res.statusMessage}"
        }
        log.error(msg, ex)
    }

    @Override
    URI getServiceLocation() {
        location
    }

    @Override
    String getDisplayName() {
        WS_NAME
    }

    @Override
    String getDescription() {
        WS_DESC
    }

    @Override
    TaskIterator createTaskIterator(Object o) {
        // no tasks for query
        null
    }
}
