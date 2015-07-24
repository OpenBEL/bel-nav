package org.openbel.kamnav.common.model

import groovy.transform.TupleConstructor
import org.openbel.framework.common.enums.FunctionEnum

@TupleConstructor
class Node {
    final String id
    final FunctionEnum fx
    final String label

    def String toString() {
        label
    }
}
