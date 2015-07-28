package org.openbel.belnav.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Constant {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    static void setLoggingExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            void uncaughtException(Thread t, Throwable e) {
                msg.error("Unhandled error: ${e.message}", e)
            }
        })
    }

    private Constant() {/* private accessors only */}
}
