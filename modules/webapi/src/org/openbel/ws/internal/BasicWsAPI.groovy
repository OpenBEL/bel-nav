package org.openbel.ws.internal

import org.openbel.ws.api.WsAPI
import wslite.soap.*

class BasicWsAPI implements WsAPI {

    @Override
    Map loadKnowledgeNetwork(name) {
        //def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def client = new SOAPClient('http://localhost:10000/openbel-ws/belframework')
        def loadMap = [name: name]
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
            new Timer().runAfter(1000, {
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
            })
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
        loadMap
    }

    @Override Map knowledgeNetworks() {
        def client = new SOAPClient('http://localhost:10000/openbel-ws/belframework')
        //def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
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

    @Override
    Map[] adjacentEdges(node) {
        return new Map[0]
    }

    @Override
    Map[] resolveNodes(name, nodes) {
        return new Map[0]
    }

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
            def name = kams.take(1).values().first()._children.find({it.name}).name._text
            println "LoadKam...Grab first one ($name)"
            println api.loadKnowledgeNetwork(name)
        }
        println "LoadKam...Does not exist"
        println api.loadKnowledgeNetwork('Does not exist')
    }
}
