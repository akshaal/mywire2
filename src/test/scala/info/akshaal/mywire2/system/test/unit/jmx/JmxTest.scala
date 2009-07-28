/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.test.unit.jmx

import java.lang.management.ManagementFactory
import javax.management.{ObjectName, Attribute}

import org.testng.annotations.Test
import org.testng.Assert._

import mywire2.system.test.unit.{BaseUnitTest, UnitTestModule, HiPriorityActor}
import mywire2.system.jmx.{JmxAttr, JmxOper, SimpleJmx}

class JmxTest extends BaseUnitTest {
    JmxTestObject

    @Test (groups=Array("unit"))
    def test () = {
        val obj = new ObjectName (JmxTestObject.jmxObjectName)
        val srv = ManagementFactory.getPlatformMBeanServer()

        // r
        assertEquals (JmxTestObject.r, 1)
        assertEquals (srv.getAttribute (obj, "r"), 1)
        JmxTestObject.r = 55
        assertEquals (srv.getAttribute (obj, "r"), 55)

        // w
        assertEquals (JmxTestObject.w, 2)
        srv.setAttribute (obj, new Attribute ("w", 10))
        assertEquals (JmxTestObject.w, 10)

        // rw
        assertEquals (JmxTestObject.rw, 3)
        assertEquals (srv.getAttribute (obj, "rw"), 3)
        JmxTestObject.rw = 66
        assertEquals (srv.getAttribute (obj, "rw"), 66)
        srv.setAttribute (obj, new Attribute ("rw", 123))
        assertEquals (srv.getAttribute (obj, "rw"), 123)
        assertEquals (JmxTestObject.rw, 123)

        // Invocation
        assertFalse (JmxTestObject.operCalled)
        // TODO: Implement ivocation
        // assertTrue (JmxTestObject.operCalled)
    }
}

object JmxTestObject extends SimpleJmx {
    var r = 1
    var w = 2
    var rw : Int = 3

    var operCalled = false
              
    override lazy val jmxObjectName = "mywire:name=jmxTestObject"

    override lazy val jmxAttributes = List (
        JmxAttr ("r",    Some (() => r),   None),
        JmxAttr ("w",    None,             Some ((x : Int) => w = x)),
        JmxAttr ("rw",   Some(() => rw),   Some ((x : Int) => rw = x))
    )

    override lazy val jmxOperations = List (
        JmxOper ("invoke", () => operCalled = true)
    )
}