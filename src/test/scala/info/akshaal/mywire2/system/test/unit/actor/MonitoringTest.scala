package info.akshaal.mywire2.system.test.unit.actor
/*
import mywire2.system.actor.HiPriorityActor
import mywire2.system.daemon.Daemon
import mywire2.system.RuntimeConstants

import mywire2.test.common.BaseTest

import org.testng.annotations.Test
import org.testng.Assert._

/**
 * @author akshaal
 */

class MonitoringTest extends BaseTest {
    @Test (groups=Array("latest"),
           dependsOnGroups=Array("indie"))
    def testBadActor () = {
        BadActor.start
        BadActor ! "Hi"
        assertFalse (Daemon.isDying, "The application must not be dying at this moment!")

        Thread.sleep (RuntimeConstants.actorsMonitoringInterval.asMilliseconds * 3)
        assertTrue (Daemon.isDying, "The application must be dying at this moment!")

        BadActor.exit
    }
}

object BadActor extends HiPriorityActor {
    def act () = {
        case x => {
            debug ("Starting to sleep")
            Thread.sleep (RuntimeConstants.actorsMonitoringInterval.asMilliseconds * 2)
            debug ("We slept well")
        }
    }
}
*/