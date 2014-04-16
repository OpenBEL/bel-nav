package org.openbel.kamnav.ui

import groovy.swing.SwingBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static javax.swing.JOptionPane.ERROR_MESSAGE

class MessagePopups {

    static void errorConnectionAccess(String url) {
        def swing = new SwingBuilder()
        String msg = "The OpenBEL server could not be reached.  Please verify\n" +
                     "the entered information:\n" +
                     "\n\tURL: $url"
        swing.optionPane(message: msg, messageType: ERROR_MESSAGE).
                createDialog(null, 'Connection Error').setVisible(true)
    }

    /**
     * Static access only.
     */
    private MessagePopups() {}
}
