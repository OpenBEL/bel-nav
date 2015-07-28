package org.openbel.ws.api

interface WsManager {

    boolean add(URI endpoint)

    boolean remove(URI endpoint)

    WsAPI get(URI endpoint)

    URI getDefault()

    boolean setDefault(URI endpoint)

    Set<URI> getAll()

    void removeAll()

    void saveConfiguration()
}