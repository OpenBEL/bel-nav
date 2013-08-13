package org.openbel.ws

import org.cytoscape.service.util.AbstractCyActivator
import org.openbel.ws.api.WsAPI
import org.openbel.ws.internal.BasicWsAPI
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) throws Exception {
        WsAPI wsAPI = new BasicWsAPI()
        registerService(bc, wsAPI, WsAPI.class, [:].asType(Properties.class))
    }
}
