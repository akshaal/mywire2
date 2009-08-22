package info.akshaal.mywire2
package system.test.unit.actor

import com.google.inject.{Guice, Injector}

import Predefs._
import system.module.Module
import system.MywireManager
import system.scheduler.Scheduler
import system.test.unit.BaseUnitTest
import system.utils.HiPriorityPool
import system.actor.{Monitoring, MonitoringActor, ActorManager, Actor}
import system.daemon.DaemonStatus

import org.testng.annotations.Test
import org.testng.Assert._

import java.lang.management.ManagementFactory
import javax.management.ObjectName

// NOTE: This test cannot use usual Test module, because it must not set
// it to dying state!, so we redefine some objects

object MonitoringTestModule extends Module {
    override val daemonStatusJmxName = "mywire:name=monitoringTestDaemonStatus"

    val injector = Guice.createInjector (MonitoringTestModule)
    val mywireManager = injector.getInstance (classOf[MywireManager])

    mywireManager.start

    val actorManager = injector.getInstance (classOf[ActorManager])
    val daemonStatus = injector.getInstance (classOf[DaemonStatus])
    val scheduler = injector.getInstance (classOf[Scheduler])
    val hiPriorityPool = injector.getInstance (classOf[HiPriorityPool])

    abstract class HiPriorityActor extends Actor (
                     scheduler = scheduler,
                     pool = hiPriorityPool)
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

        Thread.sleep (MonitoringTestModule.monitoringInterval.asMilliseconds * 4)

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
            Thread.sleep (MonitoringTestModule.monitoringInterval.asMilliseconds * 2)
            debug ("We slept well")
        }
    }
}
