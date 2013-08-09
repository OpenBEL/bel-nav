package org.openbel.ws.api

/**
 * WsAPI defines an API to interact with knowledge networks through the web.
 */
interface WsAPI {

    Map loadKnowledgeNetwork(name)

    /**
     * Returns the provided knowledge networks.
     *
     * @return {@link Map Map[]} of knowledge network properties; the returned
     * {@link Map Map[]} can be empty, never {@code null}
     */
    Map[] knowledgeNetworks()

    Map[] adjacentEdges(node)

    Map[] resolveNodes(name, nodes)

    Map[] resolveEdges(name, edges)

}