package org.openbel.kamnav.core.event

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyRow
import org.cytoscape.model.events.NetworkAddedEvent
import org.cytoscape.model.events.NetworkAddedListener
import org.cytoscape.model.events.RowSetRecord
import org.cytoscape.model.events.RowsSetEvent
import org.cytoscape.model.events.RowsSetListener
import org.cytoscape.work.TaskIterator
import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.model.Term
import org.openbel.kamnav.core.task.AddBelColumnsToCurrent
import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.bel.parser.BELParser.parseTerm
import static org.openbel.kamnav.common.util.Util.createColumn

@TupleConstructor
class BELNetworkListener implements NetworkAddedListener, RowsSetListener {

    final Expando cyr

    @Override
    void handleEvent(NetworkAddedEvent evt) {
        def missingData = evt.network.nodeList.any {
            !evt.network.getRow(it).isSet('bel.function')
        }

        if (missingData) {
            cyr.taskManager.execute(new TaskIterator(new AddBelColumnsToCurrent(null, evt.network)))
        }
    }

    @Override
    void handleEvent(RowsSetEvent evt) {
        if (!evt.containsColumn(NAME)) return
        evt.getColumnRecords(NAME).each(this.&addFunction)
    }

    private static void addFunction(RowSetRecord record) {
        createColumn(record.row.table, 'bel.function', String.class, false, null)
        def name = record.row.get(NAME, String.class)
        if (name) {
            try {
                Term t = parseTerm(name)
                if (t) {
                    record.row.set('bel.function', t.functionEnum.displayValue)
                }
            } catch (InvalidArgument e) {
                // indicates failure to parse; skip
            }
        }
    }
}
