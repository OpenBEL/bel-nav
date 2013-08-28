package org.openbel.kamnav.core.task

import org.cytoscape.model.CyRow
import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.model.Term

import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.bel.parser.BELParser.parseTerm
import groovy.transform.TupleConstructor
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

@TupleConstructor
public class AddBelColumnsToCurrent extends AbstractTask {

    final CyApplicationManager appMgr

	@Override
	public void run(TaskMonitor monitor) {
        CyNetwork cyN = appMgr.currentNetwork
        if (!cyN) return

        // create column if needed
        cyN.defaultNodeTable.getColumn('bel.function') ?:
            cyN.defaultNodeTable.createColumn('bel.function', String.class, false)

        cyN.nodeList.collect(cyN.&getRow).each(this.&addFunction)
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
