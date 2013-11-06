package org.openbel.kamnav.common.model

import groovy.transform.TupleConstructor

@TupleConstructor
class Namespace {
    final String id
    final String name
    final String prefix
    final String resourceLocation

    String toString() {
        name
    }
}
