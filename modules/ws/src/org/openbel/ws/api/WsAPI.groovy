package org.openbel.ws.api

import org.cytoscape.model.CyNetwork
import org.openbel.framework.common.enums.FunctionEnum
import org.openbel.kamnav.common.model.Node
import org.openbel.kamnav.common.model.Edge

import java.util.regex.Pattern

/**
 * WsAPI defines an API to interact with knowledge networks through the web.
 */
interface WsAPI {

    /**
     * Load a knowledge network using the unique name.  Return a {@link Map}
     * containing load results.
     * <br/>
     * The map will contain:
     * <ul>
     *     <li><strong>status</strong></li>
     *     <ul>
     *         <li>FAILED or COMPLETE</li>
     *         <li>An IN_PROCESS status prompts subsequent requests at a small rate
     *         thus it will never be returned.</li>
     *     </ul>
     *     <li><strong>message</strong></li>
     *     <ul>
     *         <li>The loaded message that reflects the <strong>status</strong>.</li>
     *     </ul>
     *     <li><strong>handle</strong> (optional)</li>
     *     <ul>
     *         <li>A string token that represents a handle to a knowledge network.</li>
     *         <li>The <strong>handle</strong> will only be present when the returned
     *         <strong>status</strong> is <strong>COMPLETE</strong>.</li>
     *     </ul>
     * </ul>
     *
     * @param name {@link String} knowledge network name; may not be {@code null}
     * @throws NullPointerException when {@code name} is {@code null}
     */
    Map loadKnowledgeNetwork(String name)

    /**
     * Returns the provided knowledge networks as a {@link Map} where the
     * unique knowledge network name is the key and the knowledge network
     * properties is the value.
     *
     * @return {@link Map} of knowledge network; the returned {@link Map} can
     * be empty, never {@code null}
     */
    Map knowledgeNetworks()

    void link(CyNetwork cyn, String name, Closure closure)

    Node[] findNodes(Pattern labelPattern, FunctionEnum... functions)

    Edge[] adjacentEdges(Node node)

    Node[] resolveNodes(String name, Node[] nodes)

    Edge[] resolveEdges(String name, Edge[] edges)
}