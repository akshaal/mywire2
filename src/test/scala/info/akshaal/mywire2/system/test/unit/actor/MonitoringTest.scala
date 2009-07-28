package info.akshaal.mywire2.system.test.unit.actor

import mywire2.Predefs._
import mywire2.system.test.unit.{BaseUnitTest, UnitTestModule}
import mywire2.system.utils.HiPriorityPool
import mywire2.system.actor.{Monitoring, MonitoringActor, ActorManager, Actor}
import mywire2.system.daemon.DaemonStatus

import org.testng.annotations.Test
import org.testng.Assert._

// XXX: This test cannot use usual Test module, because it must not set
// it to dying state!, so we redefine some objects

object MonitoringTestModule {
    object HiPriorityPoolImpl extends {
        override val threads = 2
        override val latencyLimit = 2.milliseconds
        override val executionLimit = 800.microseconds
    } with HiPriorityPool

    object MonitoringImpl extends {
        val monitoringActors = List(MonitoringActor1Impl, MonitoringActor2Impl)
    } with Monitoring

    object ActorManagerImpl extends {
        val monitoring = MonitoringImpl
    } with ActorManager

    object DaemonStatusImpl extends DaemonStatus

    class MonitoringActorImpl extends {
        override val scheduler = UnitTestModule.SchedulerImpl
        override val pool = UnitTestModule.NormalPriorityPoolImpl
        override val interval = UnitTestModule.monitoringInterval
        override val daemonStatus = DaemonStatusImpl
    } with MonitoringActor

    object MonitoringActor1Impl extends MonitoringActorImpl
    object MonitoringActor2Impl extends MonitoringActorImpl

    abstract class HiPriorityActor extends {
        override val scheduler = UnitTestModule.SchedulerImpl
        override val pool = HiPriorityPoolImpl
    } with Actor

    // Run actors
    MonitoringImpl.monitoringActors.foreach (ActorManagerImpl.startActor (_))
}

class MonitoringTest extends BaseUnitTest {
    MonitoringTestModule // We use it

    @Test (groups=Array("unit"))
    def testBadActor () = {
        MonitoringTestModule.ActorManagerImpl.startActor (BadActor)
        BadActor ! "Hi"
        assertFalse (MonitoringTestModule.DaemonStatusImpl.isDying,
                     "The application must not be dying at this moment!")
        assertFalse (MonitoringTestModule.DaemonStatusImpl.isShuttingDown,
                     "The application must not be shutting down at this moment!")

        Thread.sleep (UnitTestModule.monitoringInterval.asMilliseconds * 4)
        assertTrue (MonitoringTestModule.DaemonStatusImpl.isDying,
                    "The application must be dying at this moment!")
        assertTrue (MonitoringTestModule.DaemonStatusImpl.isShuttingDown,
                     "The application must be shutting down at this moment!")

        MonitoringTestModule.ActorManagerImpl.stopActor (BadActor)
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
