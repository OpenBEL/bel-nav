package org.openbel.belnav.ui

import org.openbel.ws.api.WsManager

import javax.swing.JDialog

public interface ConfigurationUI {

    JDialog configurationDialog(WsManager wsManager, Closure auth, Closure save)
}