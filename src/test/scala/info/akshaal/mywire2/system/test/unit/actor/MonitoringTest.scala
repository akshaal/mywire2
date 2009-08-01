package info.akshaal.mywire2
package system.test.unit.actor

import Predefs._
import system.test.unit.{BaseUnitTest, UnitTestModule}
import system.utils.HiPriorityPool
import system.actor.{Monitoring, MonitoringActor, ActorManager, Actor}
import system.daemon.DaemonStatus

import org.testng.annotations.Test
import org.testng.Assert._

import java.lang.management.ManagementFactory
import javax.management.ObjectName

// NOTE: This test cannot use usual Test module, because it must not set
// it to dying state!, so we redefine some objects

object MonitoringTestModule {
    val daemonStatusJmxName = "mywire:name=monitoringTestDaemonStatus"

    val daemonStatus = new DaemonStatus (daemonStatusJmxName)

    val hiPriorityPool =
        new HiPriorityPool (threads = 2,
                            latencyLimit = 2.milliseconds,
                            executionLimit = 800.microseconds,
                            prefs = UnitTestModule.prefs,
                            daemonStatus = daemonStatus,
                            threadPriorityChanger = UnitTestModule.threadPriorityChanger)

    class MonitoringActorImpl extends MonitoringActor (
                                 scheduler = UnitTestModule.scheduler,
                                 pool = UnitTestModule.normalPriorityPool,
                                 interval = UnitTestModule.monitoringInterval,
                                 daemonStatus = daemonStatus)

    object MonitoringActor1Impl extends MonitoringActorImpl
    object MonitoringActor2Impl extends MonitoringActorImpl

    val monitoring = new Monitoring (List(MonitoringActor1Impl,
                                          MonitoringActor2Impl))
    val actorManager = new ActorManager (monitoring)

    abstract class HiPriorityActor extends Actor (
                        scheduler = UnitTestModule.scheduler,
                        pool = hiPriorityPool)

    // Run actors
    actorManager.startActor (MonitoringActor1Impl)
    actorManager.startActor (MonitoringActor2Impl)
}

class MonitoringTest extends BaseUnitTest {
    MonitoringTestModule // We use it

    @Test (groups=Array("unit"))
    def testBadActor () = {
        val srv = ManagementFactory.getPlatformMBeanServer()
        val statusObj = new ObjectName (MonitoringTestModule.daemonStatusJmxName)

        MonitoringTestModule.actorManager.startActor (BadActor)
        BadActor ! "Hi"
        
        assertFalse (MonitoringTestModule.daemonStatus.isDying,
                     "The application must not be dying at this moment!")
        assertFalse (MonitoringTestModule.daemonStatus.isShuttingDown,
                     "The application must not be shutting down at this moment!")
        assertEquals (srv.getAttribute (statusObj, "dying"), false)
        assertEquals (srv.getAttribute (statusObj, "shuttingDown"), false)

        Thread.sleep (UnitTestModule.monitoringInterval.asMilliseconds * 4)

        assertTrue (MonitoringTestModule.daemonStatus.isDying,
                    "The application must be dying at this moment!")
        assertTrue (MonitoringTestModule.daemonStatus.isShuttingDown,
                     "The application must be shutting down at this moment!")
        assertEquals (srv.getAttribute (statusObj, "dying"), true)
        assertEquals (srv.getAttribute (statusObj, "shuttingDown"), true)

        MonitoringTestModule.actorManager.stopActor (BadActor)
    }
}

object BadActor extends MonitoringTestModule.HiPriorityActor {
    def act () = {
        case x => {
            debug ("Starting to sleep")
            Thread.sleep (UnitTestModule.monitoringInterval.asMilliseconds * 2)
            debug ("We slept well")
        }
    }
}
