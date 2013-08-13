package org.openbel.ws.internal

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
            def resMap = response.LoadKamResponse[0].flatMap()
            loadMap.status = resMap.LoadKamResponse._children.find { it.loadStatus }.loadStatus._text
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
                resMap = response.LoadKamResponse[0].flatMap()
                loadMap.status = resMap.LoadKamResponse._children.find { it.loadStatus }.loadStatus._text
                sleep(1000)
            }

            if (loadMap.status == 'FAILED') {
                loadMap.message = resMap.LoadKamResponse._children.find { it.message }.message._text
            } else if (loadMap.status == 'COMPLETE') {
                loadMap.handle = resMap.LoadKamResponse._children.find { it.handle }.handle._children[0].handle._text
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
            collect { it.flatMap() }.
            collect { it.kams }.
            groupBy { it._children.find({it.name}).name._text }.
            collectEntries { k, v -> [(k): v.first()]}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map[] adjacentEdges(node) {
        return new Map[0]
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map[] resolveNodes(name, nodes) {
        return new Map[0]
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Map[] resolveEdges(name, edges) {
        return new Map[0]
    }

    static void main(args) {
        def api = new BasicWsAPI()

        println "GetCatalog...Hash keyed by knowledge network name"
        def kams = api.knowledgeNetworks()
        println "returned map, keys: ${kams.keySet()}"
        println()

        if (kams) {
            String name = kams.take(1).values().first()._children.find({it.name}).name._text
            println "LoadKam...Grab first one ($name)"
            api.loadKnowledgeNetwork(name) { println it }
        }
        println "LoadKam...Does not exist"
        api.loadKnowledgeNetwork('Does not exist') { println it }
    }
}
