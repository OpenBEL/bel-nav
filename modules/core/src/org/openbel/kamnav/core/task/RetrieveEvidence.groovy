package org.openbel.kamnav.core.task

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.ws.api.WsAPI

import static org.openbel.kamnav.common.util.EdgeUtil.toEdge
import static org.openbel.kamnav.core.Util.createColumn
import static org.openbel.ws.api.BelUtil.belStatement
import static Boolean.TRUE
import static org.cytoscape.model.SavePolicy.DO_NOT_SAVE
import static org.cytoscape.model.CyNetwork.NAME

@TupleConstructor
class RetrieveEvidence extends AbstractTask {

    final CyNetworkView cyNv
    final WsAPI wsAPI
    final CyTableFactory fac
    final CyTableManager mgr

    @Override
    void run(TaskMonitor m) throws Exception {
        def cyN = cyNv.model

        def edges = cyN.edgeList.findAll { edge ->
            def row = cyNv.model.getRow(edge)
            row.isSet('linked') && row.get('linked', Boolean.class) == TRUE
        }
        m.title = "Show evidence for ${edges.size()} edges."
        m.progress = 0
        def incr = edges.size() / edges.size()

        def evTbl = mgr.getAllTables(true).find{it.title == 'BEL.Evidence'} ?:
            fac.createTable('BEL.Evidence', 'index', Integer.class, true, false)
        evTbl.savePolicy = DO_NOT_SAVE
        createColumn(evTbl, 'network', Long.class, true, null)
        createColumn(evTbl, 'network_name', String.class, true, null)
        createColumn(evTbl, 'edge', Long.class, true, null)
        createColumn(evTbl, 'edge source', String.class, true, null)
        createColumn(evTbl, 'edge relationship', String.class, true, null)
        createColumn(evTbl, 'edge target', String.class, true, null)
        createColumn(evTbl, 'statement', String.class, true, null)
        createColumn(evTbl, 'citation type', String.class, true, null)
        createColumn(evTbl, 'citation id', String.class, true, null)
        createColumn(evTbl, 'citation name', String.class, true, null)
        mgr.addTable(evTbl)

        def last = evTbl.allRows.max { it.get('index', Integer.class)}
        def next = last ? last.get('index', Integer.class) : 0

        // delete evidence for network if exists
        evTbl.deleteRows(evTbl.getMatchingRows('network', cyN.SUID).collect {
            it.get('index', Integer.class)
        })

        edges.collect { cyE ->
            def edge = toEdge(cyN, cyE)
            m.statusMessage = "Retrieving evidence for $edge"
            m.progress += incr
            wsAPI.getSupportingEvidence(toEdge(cyN, cyE)).each {
                it.SUID = cyE.SUID
            }
        }.flatten().each { ev ->
            def row = evTbl.getRow(next++)
            row.set('network', cyN.SUID)
            row.set('network_name', cyN.getRow(cyN).get(NAME, String.class))
            row.set('edge', ev.SUID)
            row.set('edge source', ev.edge_source)
            row.set('edge relationship', ev.edge_rel)
            row.set('edge target', ev.edge_target)
            row.set('statement', belStatement(ev))
            row.set('citation type', ev.citationType)
            row.set('citation id', ev.citationId)
            row.set('citation name', ev.citationName)
            ev.annotations.each { type, value ->
                def annotationName = "annotation: $type"
                createColumn(evTbl, annotationName, String.class, true, null)
                row.set(annotationName, value)
            }
        }
    }
}
