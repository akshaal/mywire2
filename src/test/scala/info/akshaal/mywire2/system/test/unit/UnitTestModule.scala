/*
 * TestModule.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system.test.unit

import Predefs._
import system.logger.{LogActor, LogServiceAppender}
import system.utils.{LowPriorityPool, NormalPriorityPool, HiPriorityPool,
                     ThreadPriorityChanger}
import system.scheduler.Scheduler
import system.actor.{Monitoring, MonitoringActor, ActorManager, Actor}
import system.daemon.{DaemonStatus, DeamonStatusActor}
import system.fs.FileActor
import system.dao.LogDao

abstract class HiPriorityActor extends {
    override val scheduler = UnitTestModule.SchedulerImpl
    override val pool = UnitTestModule.HiPriorityPoolImpl
} with Actor

object UnitTestModule {
    val prefsResource = "/mywire.properties"
    val monitoringInterval = 2.seconds
    val daemonStatusJmxName = "mywire:name=unitTestDaemonStatus"
    val daemonStatusUpdateInterval = 5.seconds
    val daemonStatusFile = "/tmp/mywire2-unitTest.status"

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Preferences

    val prefsX = new Prefs (prefsResource)

    // - - - - - - - -  - - - - - - - - - --  - - - - -
    // Thread priority changer

    val threadPriorityChangerX = new ThreadPriorityChanger (prefsX)

    // - - - - - - -  - - - - - - - - - - - - - - - - - -
    // Pools

    object LowPriorityPoolImpl extends {
        override val threads = 2
        override val latencyLimit = 1.seconds
        override val executionLimit = 400.milliseconds
        override val threadPriorityChanger = threadPriorityChangerX
     } with LowPriorityPool

    object NormalPriorityPoolImpl extends {
        override val threads = 2
        override val latencyLimit = 800.milliseconds
        override val executionLimit = 200.milliseconds
        override val threadPriorityChanger = threadPriorityChangerX
    } with NormalPriorityPool

    object HiPriorityPoolImpl extends {
        override val threads = 2
        override val latencyLimit = 10.milliseconds
        override val executionLimit = 800.microseconds
        override val threadPriorityChanger = threadPriorityChangerX
    } with HiPriorityPool

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Scheduler

    object SchedulerImpl extends {
        override val latencyLimit = 8.milliseconds
        override val prefs = prefsX
        override val threadPriorityChanger = threadPriorityChangerX
    } with Scheduler

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring

    object MonitoringImpl extends {
        val monitoringActors = List(MonitoringActor1Impl, MonitoringActor2Impl)
    } with Monitoring

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actor manager

    object ActorManagerImpl extends {
        val monitoring = MonitoringImpl
    } with ActorManager

    object DeamonStatusActorImpl extends {
        protected override val scheduler = SchedulerImpl
        protected override val pool = NormalPriorityPoolImpl
        protected override val interval = daemonStatusUpdateInterval
        protected override val daemonStatus = DaemonStatusImpl
        protected override val statusFile = daemonStatusFile
    } with DeamonStatusActor

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Daos

    object LogDaoImpl extends LogDao

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actors

    object LogActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = LowPriorityPoolImpl
        override val logDao = LogDaoImpl
    } with LogActor

    object FileActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = NormalPriorityPoolImpl
        override val prefs = prefsX
    } with FileActor

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Daemon

    object DaemonStatusImpl extends DaemonStatus {
        override lazy val jmxObjectName = daemonStatusJmxName
    }

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring Actors

    class MonitoringActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = NormalPriorityPoolImpl
        override val interval = monitoringInterval
        override val daemonStatus = DaemonStatusImpl
    } with MonitoringActor

    object MonitoringActor1Impl extends MonitoringActorImpl
    object MonitoringActor2Impl extends MonitoringActorImpl

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Init code

    // Init logger
    LogServiceAppender.logActor = Some(LogActorImpl)

    // Run actors
    val actors = MonitoringImpl.monitoringActors ++
                 List(LogActorImpl,
                      DeamonStatusActorImpl,
                      FileActorImpl)

    actors.foreach (ActorManagerImpl.startActor (_))

    // Start scheduling
    SchedulerImpl.start ()
}
