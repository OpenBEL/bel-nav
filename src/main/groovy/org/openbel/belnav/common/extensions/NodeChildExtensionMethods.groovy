package org.openbel.belnav.common.extensions

import groovy.util.slurpersupport.NodeChild

class NodeChildExtensionMethods {
    static Map flatMap(NodeChild self) {
        def nodeMap = [:]
        nodeMap << (self.attributes() ? [*: self.attributes()] : [:])
        nodeMap << (self.text() ? [_text: self.text()] : [:])
        nodeMap << (self.children().size() ?
                       [ _children: self.children().collect {it.flatMap()}]
                       : [:])
        [(self.name()): nodeMap]
    }

    static void main(args) {
        def xmlstr = '''
            <root id="A242">
                <group>
                    <tag name="foo">
                        <item>1</item>
                        <item>2</item>
                    </tag>
                    <tag name="bar">Text node<item>baz</item></tag>
                </group>
            </root>
        '''
        def xml = new XmlSlurper().parseText(xmlstr)
        def map = xml.flatMap()
        println map.root._children[0].group._children[0].tag._text
    }
}
