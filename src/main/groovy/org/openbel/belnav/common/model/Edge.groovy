package org.openbel.belnav.common.model

import groovy.transform.TupleConstructor

@TupleConstructor
class Edge {
    final String id
    final Node source
    final String relationship
    final Node target

    def String toString() {
        "${source} ${relationship ?: ''} ${target}"
    }
}
