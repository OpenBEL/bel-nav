package org.openbel.ws.internal

import org.openbel.kamnav.common.model.Node
import org.openbel.kamnav.common.model.Edge
import org.openbel.ws.api.WsAPI
import wslite.soap.*

/**
 * {@link WsAPI} implementation using groovy-wslite (soap xml builders).
 */
class BasicWsAPI implements WsAPI {

    /**
     * {@inheritDoc}
     */
    @Override
    void loadKnowledgeNetwork(String name, Closure closure = null) {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def loadMap = [name: name]

        Thread.start {
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

            if (closure) closure.call(loadMap)
        }
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
    Map[] adjacentEdges(Node node) {
        return new Map[0]
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map[] resolveNodes(String name, Node[] nodes) {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def loadMap = [:]
        loadKnowledgeNetwork(name) {
            loadMap << it

            def resolveMap = [handle: loadMap.handle]

            def response = client.send {
                body {
                    ResolveNodesRequest('xmlns': 'http://belframework.org/ws/schemas') {

                    }
                }
            }
            response.GetCatalogResponse.kams.
                    collect {[
                            id: it.id,
                            name: it.name,
                            description: it.description,
                            lastCompiled: it.lastCompiled
                    ]}.
                    groupBy { it.name }.
                    collectEntries { k, v -> [(k): v.first()]}
            return new Map[0]
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map[] resolveEdges(String name, Edge[] edges) {
        return new Map[0]
    }

    static void main(args) {
        def api = new BasicWsAPI()

        println "GetCatalog...Hash keyed by knowledge network name"
        def kams = api.knowledgeNetworks()
        println "returned, map keys: ${kams.keySet()}, map: $kams"
        println()

        if (kams) {
            String name = kams.take(1).values().first().name
            println "LoadKam...Grab first one ($name)"
            api.loadKnowledgeNetwork(name) { println it }
        }
        println "LoadKam...Does not exist"
        api.loadKnowledgeNetwork('Does not exist') { println it }
    }
}
