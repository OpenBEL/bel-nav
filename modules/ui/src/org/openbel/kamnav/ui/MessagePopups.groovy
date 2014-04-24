package org.openbel.kamnav.ui

import groovy.swing.SwingBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static javax.swing.JOptionPane.ERROR_MESSAGE
import static javax.swing.JOptionPane.INFORMATION_MESSAGE

class MessagePopups {

    static void errorConnectionAccess(String url) {
        def swing = new SwingBuilder()
        String msg = "The OpenBEL server could not be reached.  Please verify\n" +
                     "the entered information:\n" +
                     "\n\tURL: $url"
        swing.optionPane(message: msg, messageType: ERROR_MESSAGE).
                createDialog(null, 'Connection Error').setVisible(true)
    }

    static void info(String title, String message) {
        def swing = new SwingBuilder()
        swing.doLater {
            swing.optionPane(message: message, messageType: INFORMATION_MESSAGE).
                    createDialog(null, title).setVisible(true)
        }
    }

    /**
     * Static access only.
     */
    private MessagePopups() {}
}
