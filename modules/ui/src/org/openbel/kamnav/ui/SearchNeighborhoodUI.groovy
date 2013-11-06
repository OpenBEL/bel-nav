package org.openbel.kamnav.ui

import javax.swing.JDialog

interface SearchNeighborhoodUI {

    JDialog neighborhoodFacet(Iterator<Map> evidenceIterator,
                              Closure denormalize, Closure addEdges)
}
