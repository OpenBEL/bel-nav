package org.openbel.kamnav.common.model

import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.openbel.framework.common.enums.FunctionEnum

@TupleConstructor
@ToString
class Node {
    final String id
    final FunctionEnum fx
    final String label
}
