package org.openbel.ws.api

/**
 * WsAPI defines an API to interact with knowledge networks through the web.
 */
interface WsAPI {

    Map loadKnowledgeNetwork(name)

    /**
     * Returns the provided knowledge networks as a {@link Map} where the
     * unique knowledge network name is the key and the knowledge network
     * properties is the value.
     *
     * @return {@link Map} of knowledge network; the returned {@link Map} can
     * be empty, never {@code null}
     */
    Map knowledgeNetworks()

    Map[] adjacentEdges(node)

    Map[] resolveNodes(name, nodes)

    Map[] resolveEdges(name, edges)

}