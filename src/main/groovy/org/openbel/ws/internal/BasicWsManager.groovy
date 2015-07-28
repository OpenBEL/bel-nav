package org.openbel.ws.internal

import org.openbel.belnav.core.Activator
import org.openbel.ws.api.WsAPI
import org.openbel.ws.api.WsManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BasicWsManager implements WsManager {

    private static final String CONFIG_FILE = 'config.props'
    private static final Logger log = LoggerFactory.getLogger(getClass())
    private URI defaultURI
    private def clients = [] as Set<URI>

    final File configDir

    BasicWsManager(File configDir) {
        this.configDir = configDir
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        read(configDir.listFiles().find { it.name == CONFIG_FILE })
    }

    def static register(WsAPI wsAPI) {
        if (Activator.act) Activator.act.register(wsAPI)
    }

    @Override
    boolean add(URI endpoint) {
        if (endpoint == null) return false

        if (clients.add(endpoint)) {
            register(new BasicWsAPI(endpoint))
            log.info("Ws client added: $endpoint")
            true
        } else {
            false
        }
    }

    @Override
    boolean remove(URI endpoint) {
        if (endpoint == null) return false

        if (clients.remove(endpoint)) {
            log.info("Ws client removed: $endpoint")
            true
        } else {
            false
        }
    }

    @Override
    URI getDefault() {
        defaultURI
    }

    @Override
    boolean setDefault(URI endpoint) {
        defaultURI = endpoint
    }

    WsAPI get(URI endpoint, boolean create = false) {
        if (endpoint == null) return null

        if (clients.contains(endpoint)) return new BasicWsAPI(endpoint)
        if (create) {
            add(endpoint)
            return new BasicWsAPI(endpoint)
        }
        return null
    }

    @Override
    Set<URI> getAll() {
        new LinkedHashSet<>(clients.sort().asImmutable())
    }

    @Override
    void removeAll() {
        clients.clear()
        defaultURI = null
    }

    @Override
    void saveConfiguration() {
        write(new File(configDir, CONFIG_FILE))
    }

    private void read(File configFile) {
        if (configFile) {
            Properties props = new Properties()
            props.load(new FileInputStream(configFile));
            props.collect { k, v ->
                def tokens = v.toString().split(/,/)
                if (tokens.length == 2) {
                    try {
                        // add URI client
                        URI uri = new URI(tokens[1])
                        clients.add(uri)

                        // handle default
                        if (Boolean.TRUE.toString() == tokens[0])
                            defaultURI = uri
                    } catch (URISyntaxException e) {
                        // skip this URI
                    }
                }
            }
        }
    }

    private void write(File configFile) {
        if (configFile) {
            Properties props = new Properties()
            int c = 1
            clients.each {
                def isDefault = (defaultURI && it == defaultURI)
                props.setProperty("server.${c++}", "$isDefault,$it")
            }
            props.store(new FileOutputStream(configFile), null)
        }
    }
}