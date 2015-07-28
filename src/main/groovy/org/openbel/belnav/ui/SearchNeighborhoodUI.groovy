package org.openbel.belnav.ui

import javax.swing.*

interface SearchNeighborhoodUI {

    JDialog neighborhoodFacet(Iterator<Map> evidenceIterator,
                              Closure denormalize, Closure addEdges)
}
