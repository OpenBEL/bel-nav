package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyRow
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.model.Term

import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.bel.parser.BELParser.parseTerm
import static org.openbel.kamnav.common.util.Util.createColumn
import static org.openbel.kamnav.common.util.Util.createListColumn

@TupleConstructor
public class AddBelColumnsToCurrent extends AbstractTask {

    final CyApplicationManager appMgr
    final CyNetwork cyN

	@Override
	public void run(TaskMonitor monitor) {
        def network = cyN ?: appMgr?.currentNetwork
        if (!network) return

        // create column if needed
        createColumn(network.defaultNodeTable, 'bel.function', String.class, false, null)
        createListColumn(network.defaultNodeTable, 'namespace', String.class, false, null)
        createListColumn(network.defaultNodeTable, 'entity', String.class, false, null)

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
