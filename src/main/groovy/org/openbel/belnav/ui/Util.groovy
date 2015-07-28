package org.openbel.belnav.ui

import javax.swing.*

class Util {

    static ImageIcon icon(String path, String desc) {
        def url = Util.class.getResource(path)
        url ? new ImageIcon(url, desc) : null
    }
}
