package org.openbel.belnav.common.util

import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyRow
import org.osgi.framework.BundleContext

class Util {

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
}
