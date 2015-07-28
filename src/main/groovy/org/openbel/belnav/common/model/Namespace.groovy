package org.openbel.belnav.common.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@TupleConstructor
@EqualsAndHashCode
class Namespace {
    final String id
    final String name
    final String prefix
    final String resourceLocation

    String toString() {
        name
    }
}
