package org.openbel.ws.api

import static org.openbel.framework.common.enums.RelationshipType.DECREASES
import static org.openbel.framework.common.enums.RelationshipType.DIRECTLY_DECREASES
import static org.openbel.framework.common.enums.RelationshipType.DIRECTLY_INCREASES
import static org.openbel.framework.common.enums.RelationshipType.INCREASES
import static org.openbel.framework.common.enums.RelationshipType.RATE_LIMITING_STEP_OF
import static org.openbel.framework.common.enums.RelationshipType.fromString
import org.openbel.kamnav.common.model.Edge

/**
 * Utilities for processing bel constructs.
 */
class BelUtil {

    static def CAUSAL = [
        'increases', 'decreases', 'directlyIncreases',
        'directlyDecreases', 'rateLimitingStepOf', INCREASES, DECREASES,
        DIRECTLY_INCREASES, DIRECTLY_DECREASES, RATE_LIMITING_STEP_OF
    ]

    /**
     * Returns bel statement text for an evidence object.  This method is
     * intended to support {@link WsAPI#getSupportingEvidence(Edge)}.
     *
     * @param evidence Object
     * @return String bel statement
     */
    static def belStatement(evidence) {
        if (!evidence) return null

        def statement = [evidence.subject]
        if (evidence.edge_rel) {
            def rt = fromString(evidence.edge_rel)
            statement << (rt.abbreviation ?: rt.displayValue)
            if (evidence.objectTerm) statement << evidence.objectTerm
            else if (evidence.nestedSubject) {
                def nested = []
                nested << evidence.nestedSubject
                rt = fromString(evidence.nestedRelationship)
                statement << (rt.abbreviation ?: rt.displayValue)
                nested << evidence.nestedObject
                statement << "(${nested.join(' ')})"
            }
        }
        statement.join(' ')
    }

    /**
     * Returns whether the object is causal.
     *
     * @param obj Object - expected to be like an edge or statement
     * @return boolean {@code true} for causal bel statement, {@code false}
     * otherwise
     */
    static def isCausal(obj) {
        if (!obj) throw new NullPointerException('evidence is null')
        if (obj.hasProperty('relationship')) return obj.relationship in CAUSAL
        if (obj.hasProperty('edge_rel')) return obj.edge_rel in CAUSAL
        if (obj.hasProperty('nestedRelationship')) return obj.nestedRelationship in CAUSAL
        false
    }
}
