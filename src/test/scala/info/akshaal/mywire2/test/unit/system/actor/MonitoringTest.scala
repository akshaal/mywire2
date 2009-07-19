package info.akshaal.mywire2.system.test.unit.actor

import mywire2.system.actor.HiPriorityActor
import mywire2.system.daemon.Daemon
import mywire2.system.RuntimeConstants

import mywire2.system.test.TestHelper

import org.testng.annotations.Test
import org.testng.Assert._

/**
 * @author akshaal
 */

class MonitoringTest {
    @Test (groups=Array("latest"),
           dependsOnGroups=Array("indie"))
    def testBadActor () = {
        TestHelper.startActor (BadActor)
        BadActor ! "Hi"
        assertFalse (Daemon.isDying, "The application must not be dying at this moment!")

        Thread.sleep (TestHelper.actorsMonitoringInterval.asMilliseconds * 3)
        assertTrue (Daemon.isDying, "The application must be dying at this moment!")

        TestHelper.exitActor (BadActor)
    }
}

object BadActor extends HiPriorityActor {
    def act () = {
        case x => {
            debug ("Starting to sleep")
            Thread.sleep (TestHelper.actorsMonitoringInterval.asMilliseconds * 2)
            debug ("We slept well")
        }
    }
}
