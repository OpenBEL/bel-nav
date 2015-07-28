package org.openbel.belnav.core.task

import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static javax.swing.JOptionPane.ERROR_MESSAGE
import static javax.swing.JOptionPane.showMessageDialog

abstract class BaseTask extends AbstractTask {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor m) throws Exception {
        try {
            doRun(m)
        } catch (Throwable e) {
            msg.error("Unhandled error in task ${this.class.simpleName}: ${e.message}", e)
            showMessageDialog(null, e.message, "Task error", ERROR_MESSAGE)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void cancel() {
    }

    abstract void doRun(TaskMonitor m) throws Exception;
}
