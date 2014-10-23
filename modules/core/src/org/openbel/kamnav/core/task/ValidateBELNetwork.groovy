package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.antlr.runtime.ANTLRStringStream
import org.antlr.runtime.CharStream
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.RecognitionException
import org.antlr.runtime.TokenStream
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.view.vizmap.VisualPropertyDependency
import org.cytoscape.view.vizmap.VisualStyleFactory
import org.cytoscape.view.vizmap.mappings.DiscreteMapping
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.bel.parser.BELStatementLexer
import org.openbel.framework.common.bel.parser.BELStatementParser
import org.openbel.framework.common.model.Statement
import org.openbel.kamnav.ui.MessagePopups
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.Color
import java.math.RoundingMode

import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.model.CyEdge.INTERACTION
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*
import static org.openbel.framework.common.BELUtilities.noLength
import static org.openbel.kamnav.common.util.Util.createColumn

/**
 * ValidateBELNetwork provides a {@link org.cytoscape.work.Task} to validate the
 * syntax of the {@link org.cytoscape.model.CyNode} and {@link org.cytoscape.model.CyEdge}
 * of a {@link org.cytoscape.model.CyNetwork}.
 */
@TupleConstructor
class ValidateBELNetwork extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger("CyUserMessages")

    final CyNetworkView cyNv
    final CyEventHelper cyEventHelper
    final VisualStyleFactory vsFac
    final VisualMappingManager vmManager
    final VisualMappingFunctionFactory discreteFxFac

    @Override
    void run(TaskMonitor monitor) throws Exception {
        def cyN = cyNv.model
        if (cyN.nodeCount == 0 && cyN.edgeCount == 0) {
            log.error("Network is empty.")
            return
        }

        def msg = "Network: ${cyN.getRow(cyN).get(NAME, String.class)}"
        msg += "\n\nBEL Validation"
        if (cyN.nodeCount > 0) {
            def nvm = nodeValidityMap(cyN)
            def ns = summarize(nvm, cyN.nodeCount)
            msg += "\n\n    Valid node percentage: ${ns.validPercentage} (${ns.validRatio})"
            msg += "\n    Invalid node percentage: ${ns.invalidPercentage} (${ns.invalidRatio})"

            nvm[true].each {
                def (wasValid, nodeMsg, node) = it
                createColumn(cyN.defaultNodeTable, 'valid bel', String.class, true, null)
                cyN.getRow(node).set('valid bel', Boolean.TRUE.toString())
            }
            nvm[false].each {
                def (wasValid, nodeMsg, node) = it
                createColumn(cyN.defaultNodeTable, 'valid bel', String.class, true, null)
                cyN.getRow(node).set('valid bel', Boolean.FALSE.toString())
                if (!wasValid) {
                    createColumn(cyN.defaultNodeTable, 'validation error', String.class, true, null)
                    cyN.getRow(node).set('validation error', nodeMsg)
                }
            }
        }

        if (cyN.edgeCount > 0) {
            def evm = edgeValidityMap(cyN)
            def es = summarize(evm, cyN.edgeCount)
            msg += "\n\n    Valid edge percentage: ${es.validPercentage} (${es.validRatio})"
            msg += "\n    Invalid edge percentage: ${es.invalidPercentage} (${es.invalidRatio})"

            evm[true].each {
                def (wasValid, edgeMsg, edge) = it
                createColumn(cyN.defaultEdgeTable, 'valid bel', String.class, true, null)
                cyN.getRow(edge).set('valid bel', Boolean.TRUE.toString())
            }
            evm[false].each {
                def (wasValid, edgeMsg, edge) = it
                createColumn(cyN.defaultEdgeTable, 'valid bel', String.class, true, null)
                cyN.getRow(edge).set('valid bel', Boolean.FALSE.toString())
                if (!wasValid) {
                    createColumn(cyN.defaultEdgeTable, 'validation error', String.class, true, null)
                    cyN.getRow(edge).set('validation error', edgeMsg)
                }
            }
        }

        def vs = vmManager.allVisualStyles.find { it.title == 'Node/Edge Validation' }
        if (vs == null) {
            def kamStyle = vmManager.allVisualStyles.find {
                it.title == 'KAM Visualization'
            }
            vs = kamStyle ? vsFac.createVisualStyle(kamStyle) :
                    vsFac.createVisualStyle('Node/Edge Validation')
            vs.title = 'Node/Edge Validation'

            def lock = vs.allVisualPropertyDependencies.find { it.idString == 'nodeSizeLocked' }
            if (lock) lock.setDependency(false)
            vmManager.addVisualStyle(vs)
        }

        def borderPaint = discreteFxFac.createVisualMappingFunction(
                'valid bel', String.class, NODE_BORDER_PAINT) as DiscreteMapping
        borderPaint.putMapValue(false.toString(), Color.red)
        def borderWidth = discreteFxFac.createVisualMappingFunction(
                'valid bel', String.class, NODE_BORDER_WIDTH) as DiscreteMapping
        borderWidth.putMapValue(false.toString(), 5.0)
        def edgeColor = discreteFxFac.createVisualMappingFunction(
                'valid bel', String.class, EDGE_STROKE_UNSELECTED_PAINT) as DiscreteMapping
        edgeColor.putMapValue(false.toString(), Color.red)
        def edgePaint = discreteFxFac.createVisualMappingFunction(
                'valid bel', String.class, EDGE_PAINT) as DiscreteMapping
        edgePaint.putMapValue(false.toString(), Color.red)

        vs.addVisualMappingFunction(borderPaint)
        vs.addVisualMappingFunction(borderWidth)
        vs.addVisualMappingFunction(edgeColor)
        vs.addVisualMappingFunction(edgePaint)

        cyEventHelper.flushPayloadEvents()
        vmManager.setVisualStyle(vs, cyNv)
        vs.apply(cyNv)
        cyNv.updateView()

        MessagePopups.info("Validation Results", msg)
    }

    static Map<Boolean, List> nodeValidityMap(CyNetwork cyN) {
        cyN.nodeList.collect {
            def nodeLabel = cyN.getRow(it).get(NAME, String.class)
            validTerm(nodeLabel) + it
        }.groupBy {
            def (wasValid, msg, node) = it
            wasValid
        }
    }

    static Map<Boolean, List> edgeValidityMap(CyNetwork cyN) {
        cyN.edgeList.collect {
            def source = cyN.getRow(it.source).get(NAME, String.class)
            def rel = cyN.getRow(it).get(INTERACTION, String.class)
            def target = cyN.getRow(it.target).get(NAME, String.class)
            def edgeLabel = "$source $rel $target"
            validStatement(edgeLabel) + it
        }.groupBy {
            def (wasValid, msg, edge) = it
            wasValid
        }
    }

    static def validTerm(String term) {
        if (noLength(term)) return [false, "Missing term"]

        try {
            CharStream stream = new ANTLRStringStream(term)
            BELStatementLexer lexer = new BELStatementLexer(stream)
            TokenStream tokenStream = new CommonTokenStream(lexer)
            BELStatementParser bsp = new BELStatementParser(tokenStream)
            Statement ret = bsp.statement().r
            if (!ret.subjectOnly)
                return [false, "Not a BEL term"]
            return [true, 'BEL term is valid']
        } catch (InvalidArgument e) {
            switch(e.message) {
                case "function enum is null":
                    return [false, "Missing/Invalid BEL function"]
                case "arg is null":
                    return [false, "Missing/Invalid argument in BEL term"]
                default:
                    return [false, e.message]
            }
        } catch (RecognitionException e) {
            return [
                false,
                "Parsing error at character: ${e.charPositionInLine}, token: ${e.token}"
            ]
        } catch (Throwable e) {
            return [false, e.message]
        }
    }

    static def validStatement(String statement) {
        if (noLength(statement)) return [false, "Missing statement"]

        // HACK - Replace compiler relationships with " increases " because
        //        parser does not recognize them.
        String cleaned = new String(statement)
        [
            'actsIn', 'hasComponent', 'hasMember', 'hasModification',
            'hasProduct', 'hasVariant', 'includes', 'isA', 'reactantIn',
            'translocates'
        ].each {
            cleaned = cleaned.replace(it, " increases ")
        }

        try {
            CharStream stream = new ANTLRStringStream(cleaned)
            BELStatementLexer lexer = new BELStatementLexer(stream)
            TokenStream tokenStream = new CommonTokenStream(lexer)
            BELStatementParser bsp = new BELStatementParser(tokenStream)
            Statement ret = bsp.statement().r
            if (!ret.statementTriple)
                return [false, "Not a BEL statement"]
            return [true, 'BEL statement is valid']
        } catch (InvalidArgument e) {
            switch(e.message) {
                case "function enum is null":
                    return [false, "Missing/Invalid BEL function"]
                case "arg is null":
                    return [false, "Missing/Invalid argument in BEL term"]
                default:
                    return [false, e.message]
            }
        } catch (RecognitionException e) {
            return [
                    false,
                    "Parsing error at character: ${e.charPositionInLine}, token: ${e.token}"
            ]
        } catch (Throwable e) {
            return [false, e.message]
        }
    }

    private static def summarize(Map<Boolean, List> validityMap, int total) {
        if (total == 0) return [:].withDefault {""}

        def invalid = (validityMap[false] ?: []).size()
        def invalidPercentage = new BigDecimal(
                ((double) invalid / total) * 100.0
        ).setScale(2, RoundingMode.HALF_UP).doubleValue()
        def valid = (validityMap[true] ?: []).size()
        def validPercentage = new BigDecimal(
                ((double) valid / total) * 100.0
        ).setScale(2, RoundingMode.HALF_UP).doubleValue()

        [
            validRatio: "$valid / $total",
            validPercentage: validPercentage,
            invalidRatio: "$invalid / $total",
            invalidPercentage: invalidPercentage
        ]
    }
}
