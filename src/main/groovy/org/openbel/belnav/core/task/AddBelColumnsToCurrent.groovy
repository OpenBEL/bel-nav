package org.openbel.belnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyRow
import org.cytoscape.work.TaskMonitor
import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.model.Term

import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.bel.parser.BELParser.parseTerm

@TupleConstructor
public class AddBelColumnsToCurrent extends BaseTask {

    final CyApplicationManager appMgr
    final CyNetwork cyN

	@Override
	public void doRun(TaskMonitor m) {
        def network = cyN ?: appMgr?.currentNetwork
        if (!network) return

        // create column if needed
        network.defaultNodeTable.getColumn('bel.function') ?:
            network.defaultNodeTable.createColumn('bel.function', String.class, false)

        network.nodeList.collect(network.&getRow).each(this.&addFunction)
	}

    private void addFunction(CyRow row) {
        def name = row.get(NAME, String.class)
        if (name) {
            try {
                Term t = parseTerm(name)
                if (t) {
                    row.set('bel.function', t.functionEnum.displayValue)
                }
            } catch (InvalidArgument e) {
                // indicates failure to parse; skip
            }
        }
    }
}
