package org.openbel.kamnav.core

import org.cytoscape.model.*
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import org.openbel.ws.api.WsAPI
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView

import static java.lang.Boolean.TRUE
import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.model.SavePolicy.DO_NOT_SAVE
import static org.openbel.kamnav.common.util.EdgeUtil.toEdge
import static org.openbel.kamnav.common.util.Util.createColumn
import static org.openbel.kamnav.core.Constant.*

class Util {

    def static contributeVisualStyles(VisualMappingManager visMgr,
                               LoadVizmapFileTaskFactory vf) {
        // delete/add knowledge network styles (idempotent)
        visMgr.allVisualStyles.findAll { it.title in STYLE_NAMES }.each(visMgr.&removeVisualStyle)
        vf.loadStyles((InputStream) Util.class.getResourceAsStream(STYLE_PATH))
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
        createColumn(evTbl, 'citation reference', String.class, true, null)
        createColumn(evTbl, 'citation name', String.class, true, null)
        createColumn(evTbl, 'citation comment', String.class, true, null)
        createColumn(evTbl, 'citation date', String.class, true, null)
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
        def edge = toEdge(cyN, cyE)
        api.getSupportingEvidence(edge).each {
            def row = evTbl.getRow(SUIDFactory.nextSUID)
            row.set('network', cyN.SUID)
            row.set('network_name', cyN.getRow(cyN).get(NAME, String.class))
            row.set('edge', cyE.SUID)
            row.set('edge source', edge.source.label)
            row.set('edge relationship', edge.relationship)
            row.set('edge target', edge.target.label)
            row.set('statement', it.statement)
            row.set('citation type', it.citationType)
            row.set('citation reference', it.citationId)
            row.set('citation name', it.citationName)
            row.set('citation comment', it.citationComment)
            row.set('citation date', it.citationDate)
            it.annotations.each { type, value ->
                def annotationName = "annotation: $type"
                createColumn(evTbl, annotationName, String.class, true, null)
                row.set(annotationName, value)
            }
        }
    }

    /**
     * Get the current {@link CyNetwork network} and {@link CyNetworkView view}.
     * <br><br>
     * If {@code create} is {@code true} then create both and assign as current
     * to {@link CyApplicationManager}.
     *
     * @param cyr {@link Expando} containing cytoscape service refs
     * @param create {@code true} to create network and view; defaults to {@code false}
     * @return {@link List} of size 2;
     * index 0 is {@link CyNetwork}; index 1 is {@link CyNetworkView}
     */
    static def getCurrentNetwork(Expando cyr, create = false) {
        def cyN = cyr.cyApplicationManager.currentNetwork
        def cyNv = cyr.cyApplicationManager.currentNetworkView

        if (!cyN && !create) return [null, null]
        if (!cyNv && !create) return [cyN, null]

        if (!cyN) {
            cyN = cyr.cyNetworkFactory.createNetwork()
            cyN.getRow(cyN).set(NAME, 'Network')
            cyr.cyNetworkManager.addNetwork(cyN)
        }

        if (!cyNv) {
            cyNv = cyr.cyNetworkViewFactory.createNetworkView(cyN)
            cyr.cyNetworkViewManager.addNetworkView(cyNv)
        }

        cyr.cyApplicationManager.currentNetwork = cyN
        cyr.cyApplicationManager.currentNetworkView = cyNv

        [cyN, cyNv]
    }
}
