package org.openbel.kamnav.ui

import groovy.swing.SwingBuilder

import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.application.swing.CytoPanelName
import org.cytoscape.service.util.AbstractCyActivator
import org.osgi.framework.BundleContext

import javax.swing.Icon
import javax.swing.JPanel
import java.awt.Component

class Builders {

    SwingBuilder cyswing() {
        def swing = new SwingBuilder()
        swing.registerFactory('cypanel', [
            newInstance: {
                builder, name, value, attrs ->
                    new CyPanel()
             }
        ] as Factory)

        swing.registerBeanFactory('cypanel', CyPanel.class)
        swing
    }

    class CyPanel extends JPanel implements CytoPanelComponent {
        String title = 'Default'
        CytoPanelName cytoPanelName = CytoPanelName.WEST
        Component component = this
        Icon icon = null
    }
}
