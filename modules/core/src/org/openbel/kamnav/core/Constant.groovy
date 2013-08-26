package org.openbel.kamnav.core

/**
 * Provides read-only constant values.
 */
class Constant {

    /**
     * Classpath location of additional visual styles.
     */
    static final String STYLE_PATH = '/style.xml'

    /**
     * Colorful visualization for Navigator.
     */
    static final String NAV_VIS = 'KAM Visualization'

    /**
     * Black and white visualization for Navigator.
     */
    static final String NAV_VIS_MINIMAL = 'KAM Visualization Minimal'

    /**
     * Additional visual style names.
     */
    static final String[] STYLE_NAMES = [NAV_VIS, NAV_VIS_MINIMAL]

    private Constant() {/* private accessors only */}
}
