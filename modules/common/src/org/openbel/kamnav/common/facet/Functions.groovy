package org.openbel.kamnav.common.facet

class Functions {

    /**
     * Describes the faceted/filtered fields of each item.  This will most
     * likely be a subset of the item structure.
     *
     * <p>
     * For example the map {@code [foo: [bar: [baz: 1]]]} would yield
     * {@code [baz: 1]} if {@code fieldDescription} {@link Closure} returned
     * only the {@code baz} field.
     * </p>
     *
     * @param items an iterable object
     * @param fieldDescription {@link Closure} that given an item object
     * yields a description object
     * @return field description objects; cardinality of return and
     * {@code items} will be equal
     */
    static describe(items, Closure fieldDescription) {
        if (!fieldDescription) return []
        items.collect {fieldDescription.call(it)}
    }

    /**
     * Aggregate field descriptions into a facet object, grouped by field keys,
     * that counts occurrences of every field value.
     *
     * <p>
     * For example the field descriptions:
     * <pre>
     * [
     *     [foo: 'A', bar: 'B', baz: 'C'],
     *     [foo: 'A', bar: 'B', baz: 'C'],
     *     [foo: 'a', bar: 'b', baz: 'c']
     * ]
     * </pre>
     * Would yield the facet:
     * <pre>
     * [
     *     foo: [ A: [count: 2], a: [count: 1]],
     *     bar: [ B: [count: 2], b: [count: 1]],
     *     baz: [ C: [count: 2], c: [count: 1]]
     * ]
     * </pre>
     *
     * @param fieldDescriptions field description objects
     * @return facet object
     */
    static facet(fieldDescriptions) {
        Map facets = [:].withDefault {
            [:].withDefault {[
                count: 0,
                filterComparison: 'unset'
            ]}
        }
        fieldDescriptions.inject(facets) { result, val ->
            val.each { k, v ->
                if (iterable(v)) {
                    v.each {
                        result[k][it].count++
                        result[k][it].value = it
                    }
                } else {
                    result[k][v].count++
                    result[k][v].value = v
                }
            }
            result
        }
    }


    /**
     * Merge facets by a foldLeft from left to right.
     *
     * @param facets
     * @return
     */
    static mergeFacets(facets) {
        facets.tail().inject(facets.head()) { accumulator, next ->
            next.each { k, v ->
                if (!(k in accumulator)) {
                    accumulator[k] = v
                } else {
                    v.each {
                        if (!(it.key in accumulator[k])) {
                            accumulator[k][it.key] = it.value
                        } else {
                            accumulator[k][it.key].count += (it.value.count ?: 0)
                            accumulator[k][it.key].value = it.value.value
                            accumulator[k][it.key].filterComparison = it.value.filterComparison
                        }
                    }
                }
            }
            accumulator
        }
        facets.head()
    }

    /**
     * Applies the {@code facets} to the {@code fieldDescriptions}
     * and filters that corresponding {@code items} if they all match.
     *
     * @param items collection of items
     * @param fieldDescriptions collection of field descriptions
     * @param facets facet object
     * @return filtered items
     */
    static filter(items, fieldDescriptions, facets) {
        if (items.size() != fieldDescriptions.size()) return null

        def inclusion = {
            def fieldValues = it.value.findAll {
                k, v -> v.filterComparison == 'inclusion'
            }.collect {it.key}
            [it.key, fieldValues]
        }.memoize()
        def exclusion = {
            def fieldValues = it.value.findAll {
                k, v -> v.filterComparison == 'exclusion'
            }.collect {it.key}
            [it.key, fieldValues]
        }.memoize()

        // factor out includes and excludes; find all where values [] is true
        def facetValueIncludes = facets.collectEntries(inclusion).findAll {k, v -> v}
        def facetValueExcludes = facets.collectEntries(exclusion).findAll {k, v -> v}

        [fieldDescriptions, items].transpose().findAll { desc, item ->
            // match on included facets
            def matchedIncludes = facetValueIncludes.every { field, selectedValues ->
                [desc[field]].flatten().any {it in selectedValues}
            }
            if (!matchedIncludes) return false

            // match on excluded facets
            facetValueExcludes.every { field, selectedValues ->
                !selectedValues.any {it in [desc[field]].flatten()}
            }
        }.collect { it[1] }
    }

    /**
     * Return a {@link Closure} (Object -> Boolean) that represents the
     * predicate filter for some {@code value}.
     *
     * @param values single or collection of objects
     * @return predicate filter {@link Closure}
     */
    static filterFunction(value) {
        def distinctValues = [value].flatten() as Set

        // numerical range fields
        if (distinctValues.every {it instanceof Number}) {
            def min = distinctValues.min().doubleValue()
            def max = distinctValues.max().doubleValue()
            if (distinctValues.size() == 1)
                return { val -> [val].flatten().any {
                    (min..max).containsWithinBounds(it)
                }}
            else
                return { val -> [val].flatten().every {
                    (min..max).containsWithinBounds(it)
                }}
        }

        // binary fields
        if (distinctValues.every {it instanceof Boolean}) {
            if (distinctValues.size() == 1)
                return { val -> [val].flatten().any {it in distinctValues } }
            else
                return { val -> [val].flatten().every {it in distinctValues } }
        }

        // default; treat field as strings
        if (distinctValues.size() == 1)
            return { val -> [val].flatten().any { it in distinctValues } }
        else {
            return { val -> [val].flatten().every { it in distinctValues } }
        }
    }

    /**
     * Return {@code true} if the object argument is an iterable collection
     * and {@code false} if not.
     *
     * @param obj object
     * @return {@code boolean}
     */
    static boolean iterable(obj) {
        [Collection, Object[]].any {it.isAssignableFrom(obj.class)}
    }

    // Helper Functions: Halt! Ahead there be side-effects.

    static includeFacetValues(facets, key, values) {
        mutateFacetValues(facets, key, values, 'inclusion')
    }

    static excludeFacetValues(facets, key, values) {
        mutateFacetValues(facets, key, values, 'exclusion')
    }

    static unsetFacetValues(facets, key, values) {
        mutateFacetValues(facets, key, values, 'unset')
    }

    private static mutateFacetValues(facets, key, values, filterComparison) {
        if (key in facets) {
            def vals = [values].flatten()
            facets[key].findAll {it.key in vals}.each { k, v ->
                v.filterComparison = filterComparison
            }
        }
        facets
    }
}
