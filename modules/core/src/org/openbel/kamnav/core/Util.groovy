package org.openbel.kamnav.core

import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.model.SUIDFactory
import org.openbel.ws.api.WsAPI
import org.osgi.framework.BundleContext
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import static java.lang.Boolean.TRUE
import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.model.SavePolicy.DO_NOT_SAVE
import static org.openbel.kamnav.common.util.EdgeUtil.toEdge
import static org.openbel.kamnav.core.Constant.*
import static org.openbel.ws.api.BelUtil.belStatement

class Util {

    def static contributeVisualStyles(VisualMappingManager visMgr,
                               LoadVizmapFileTaskFactory vf) {
        // delete/add knowledge network styles (idempotent)
        visMgr.allVisualStyles.findAll { it.title in STYLE_NAMES }.each(visMgr.&removeVisualStyle)
        vf.loadStyles(Util.class.getResourceAsStream(STYLE_PATH))
    }

    static Expando cyReference(BundleContext bc, Closure cyAct, Class<?>[] ifaces) {
        Expando e = new Expando()
        ifaces.each {
            def impl = cyAct.call(bc, it)
            def name = it.simpleName
            e.setProperty(name[0].toLowerCase() + name[1..-1], impl)
        }
        e
    }

    static CyColumn createColumn(table, name, type, immutable, defaultValue) {
        name = "$name"
        table.getColumn(name) ?: (table.createColumn(name, type, immutable, defaultValue))
        table.getColumn(name)
    }

    static CyColumn createListColumn(table, name, listElementType, immutable,
                                     defaultValue) {
        name = "$name"
        table.getColumn(name) ?: (table.createListColumn(name, listElementType, immutable, defaultValue))
        table.getColumn(name)
    }

    static <T> void setAdd(CyRow row, String name, Class<T> type, T element) {
        def list = row.getList(name, type, [])
        if (!list.contains(element)) {
            list.add(element)
            row.set(name, list)
        }
    }

    static <T> void listAdd(CyRow row, String name, Class<T> type, T element) {
        def list = row.getList(name, type, [])
        list.add(element)
        row.set(name, list)
    }

    static CyTable getOrCreateEvidenceTable(CyTableManager mgr, CyTableFactory fac) {
        def evTbl = mgr.getAllTables(true).find{it.title == 'BEL.Evidence'} ?:
            fac.createTable('BEL.Evidence', 'SUID', Long.class, true, false)
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

        evTbl
    }

    static void clearEvidenceTable(CyTableManager mgr, CyTableFactory fac, CyNetwork cyN) {
        def evTbl = getOrCreateEvidenceTable(mgr, fac)

        // delete evidence for network if exists
        evTbl.deleteRows(evTbl.getMatchingRows('network', cyN.SUID).collect {
            it.get('SUID', Long.class)
        })
    }

    static void addEvidenceForEdge(CyTableManager mgr, CyTableFactory fac,
                                   WsAPI api, CyNetwork cyN, CyEdge cyE) {
        // no-op if the edge is not linked
        if (cyN.getRow(cyE).get('linked', Boolean.class) != TRUE) return

        def evTbl = getOrCreateEvidenceTable(mgr, fac)

        // remove previously retrieved evidence for this edge
        evTbl.deleteRows(evTbl.getMatchingRows('edge', cyE.SUID).collect {
            it.get('SUID', Long.class)
        })

        // add to evidence table for this edge
        api.getSupportingEvidence(toEdge(cyN, cyE)).each {
            it.SUID = cyE.SUID
        }.flatten().each { ev ->
            def row = evTbl.getRow(SUIDFactory.nextSUID)
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
