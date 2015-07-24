package org.openbel.ws.internal

import groovy.transform.PackageScope
import org.cytoscape.application.CyApplicationConfiguration
import org.cytoscape.service.util.AbstractCyActivator
import org.openbel.ws.api.WsAPI
import org.openbel.ws.api.WsManager
import org.osgi.framework.BundleContext

import static org.openbel.kamnav.common.Constant.setLoggingExceptionHandler

class Activator extends AbstractCyActivator {

    static Activator act;
    private static BundleContext ctx = null;

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) throws Exception {
        ctx = bc;
        act = this

        CyApplicationConfiguration cyAppConfig = getService(bc, CyApplicationConfiguration.class)
        File configDir = cyAppConfig.getAppConfigurationDirectoryLocation(Activator.class)
        WsManager wsManager = new BasicWsManager(configDir)
        registerService(bc, wsManager, WsManager.class, [:] as Properties)

        setLoggingExceptionHandler()
    }

    @PackageScope
    void register(WsAPI wsAPI) {
        if (ctx == null) throw new IllegalStateException("ctx is null");
        registerAllServices(ctx, wsAPI, ['uri': wsAPI.serviceLocation.toString()] as Properties)
    }
}
