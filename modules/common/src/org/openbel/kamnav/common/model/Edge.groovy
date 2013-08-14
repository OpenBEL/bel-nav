package org.openbel.kamnav.common.model

import groovy.transform.TupleConstructor

@TupleConstructor
class Edge {
    final Node source
    final String relationship
    final Node target
}
