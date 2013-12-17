package org.openbel.kamnav.common.model

import groovy.transform.TupleConstructor
import org.openbel.framework.common.enums.RelationshipType

@TupleConstructor
class Edge {
    final String id
    final Node source
    final RelationshipType relationship
    final Node target

    def String toString() {
        "${source} ${relationship.abbreviation ?: relationship.displayValue} ${target}"
    }
}
