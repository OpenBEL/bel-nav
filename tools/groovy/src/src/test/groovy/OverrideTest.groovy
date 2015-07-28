/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package groovy

class OverrideTest extends GroovyTestCase {
    void testHappyPath() {
        assertScript """
            abstract class Parent<T> {
                abstract method()
                void methodTakeT(T t) { }
                T methodMakeT() { return null }
            }

            interface Intf<U> {
                def method4()
                void method5(U u)
                U method6()
            }

            interface IntfString extends Intf<String> {}

            class OverrideAnnotationTest extends Parent<Integer> implements IntfString {
                @Override method() {}
                @Override void methodTakeT(Integer arg) {}
                @Override Integer methodMakeT() {}
                @Override method4() {}
                @Override void method5(String arg) {}
                @Override String method6() {}
            }

            new OverrideAnnotationTest()
        """
    }

    void testUnhappyPath() {
        def message = shouldFail """
            abstract class Parent<T> {
                abstract method()
                void methodTakeT(T t) { }
                T methodMakeT() { return null }
            }

            interface Intf<U> {
                def method4()
                void method5(U u)
                U method6()
            }

            interface IntfString extends Intf<String> {}

            class OverrideAnnotationTest extends Parent<Integer> implements IntfString {
                @Override method() {}
                @Override void methodTakeT(arg) {}
                @Override Double methodMakeT() {}
                @Override method4() {}
                @Override void method5(String arg) {}
                @Override String method6() {}
            }

            new OverrideAnnotationTest()
        """
        assert message.contains(/The return type of java.lang.Double methodMakeT() in OverrideAnnotationTest is incompatible with java.lang.Integer in Parent/)
        assert message.contains(/Method 'methodTakeT' from class 'OverrideAnnotationTest' does not override method from its superclass or interfaces but is annotated with @Override./)
    }

    void testGroovy6654() {
        assertScript '''
class Base<T> {
    void foo(T t) {}
}

class Derived extends Base<String> {
    @Override
    void foo(String s) {}
}
def d = new Derived()
'''
    }

    void testSpuriousMethod() {
        def message = shouldFail """
            interface Intf<U> {
                def method()
            }

            interface IntfString extends Intf<String> {}

            class HasSpuriousMethod implements IntfString {
                @Override method() {}
                @Override someOtherMethod() {}
            }
        """
        assert message.contains("Method 'someOtherMethod' from class 'HasSpuriousMethod' does not override method from its superclass or interfaces but is annotated with @Override.")
    }

    void testBadReturnType() {
        def message = shouldFail """
            interface Intf<U> {
                def method()
                U method6()
            }

            interface IntfString extends Intf<String> {}

            class HasMethodWithBadReturnType implements IntfString {
                @Override method() {}
                @Override methodReturnsObject() {}
            }
        """
        assert message.contains("Method 'methodReturnsObject' from class 'HasMethodWithBadReturnType' does not override method from its superclass or interfaces but is annotated with @Override.")
    }

    void testBadArgType() {
        def message = shouldFail """
            interface Intf<U> {
                def method()
                void method6(U u)
            }

            interface IntfString extends Intf<String> {}

            class HasMethodWithBadArgType implements IntfString {
                @Override method() {}
                @Override void methodTakesObject(arg) {}
            }
        """
        assert message.contains("Method 'methodTakesObject' from class 'HasMethodWithBadArgType' does not override method from its superclass or interfaces but is annotated with @Override.")
    }
}
