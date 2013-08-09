package org.openbel.ws.internal

import org.openbel.ws.api.WsAPI
import wslite.soap.*

class BasicWsAPI implements WsAPI {

    @Override
    Map loadKnowledgeNetwork(name) {
        return null
    }

    @Override Map[] knowledgeNetworks() {
        def client = new SOAPClient('http://demo.openbel.org/openbel-ws/belframework')
        def response = client.send {
            body {
                GetCatalogRequest('xmlns': 'http://belframework.org/ws/schemas')
            }
        }
        response.GetCatalogResponse.kams.collect {
            [
                id: it.id,
                name: it.name,
                description: it.description,
                lastCompiled: it.lastCompiled
            ]
        } as Map[]
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
        def kams = new BasicWsAPI().knowledgeNetworks()
        println kams
    }
}
