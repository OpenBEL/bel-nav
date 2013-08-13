package org.openbel.kamnav.main

import groovy.swing.SwingBuilder

import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.application.swing.CytoPanelName
import org.cytoscape.service.util.AbstractCyActivator
import org.osgi.framework.BundleContext

import javax.swing.Icon
import javax.swing.JPanel
import java.awt.Component

class Activator extends AbstractCyActivator {

    void start(BundleContext bc) {
        def swing = new SwingBuilder()
        swing.registerFactory('cypanel', [
            newInstance: {
                builder, name, value, attrs ->
                    new CyPanel()
            }
        ] as AbstractFactory)

        swing.registerBeanFactory('cypanel', CyPanel.class)
        registerService(bc, swing.cypanel(), CytoPanelComponent.class, new Properties())
    }

    class CyPanel extends JPanel implements CytoPanelComponent {

        @Override
        Component getComponent() {
            return this
        }

        @Override
        CytoPanelName getCytoPanelName() {
            return CytoPanelName.WEST
        }

        @Override
        String getTitle() {
            return "Tools"
        }

        @Override
        Icon getIcon() {
            return null
        }
    }
}
