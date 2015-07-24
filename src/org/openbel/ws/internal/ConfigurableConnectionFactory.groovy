package org.openbel.ws.internal

import groovy.transform.TupleConstructor
import wslite.http.HTTPConnectionFactory

@TupleConstructor()
class ConfigurableConnectionFactory extends HTTPConnectionFactory {

    def final int connectTimeout
    def final int readTimeout

    ConfigurableConnectionFactory(connectTimeout = 10000, readTimeout = 60000) {
        this.connectTimeout = connectTimeout
        this.readTimeout = readTimeout
    }

    @Override
    def getConnection(URL url, Proxy proxy=Proxy.NO_PROXY) {
        HttpURLConnection conn = (HttpURLConnection) super.getConnection(url, proxy)
        conn.connectTimeout = connectTimeout
        conn.readTimeout = readTimeout
        conn.instanceFollowRedirects = true
        conn
    }
}