/*
 * Module.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system
package module

import Predefs._
import logger.{LogActor, LogServiceAppender}
import utils.{LowPriorityPool, NormalPriorityPool, HiPriorityPool, TimeUnit,
              ThreadPriorityChanger}
import scheduler.Scheduler
import actor.{Monitoring, MonitoringActor, ActorManager, Actor}
import daemon.{DaemonStatus, DeamonStatusActor}
import fs.FileActor
import dao.LogDao

trait Module {
    val prefsResource = "/mywire.properties"

    val monitoringInterval : TimeUnit
    val monitoringActorsCount : Int

    val lowPriorityPoolThreads : Int
    val lowPriorityPoolLatencyLimit : TimeUnit
    val lowPriorityPoolExecutionLimit : TimeUnit

    val normalPriorityPoolThreads : Int
    val normalPriorityPoolLatencyLimit : TimeUnit
    val normalPriorityPoolExecutionLimit : TimeUnit

    val hiPriorityPoolThreads : Int
    val hiPriorityPoolLatencyLimit : TimeUnit
    val hiPriorityPoolExecutionLimit : TimeUnit

    val schedulerLatencyLimit : TimeUnit

    val daemonStatusJmxName = "mywire:name=status"
    val daemonStatusUpdateInterval : TimeUnit
    val daemonStatusFile : String

    require (daemonStatusUpdateInterval > monitoringInterval * 2,
             "daemonStatusUpdateInterval must greater than 2*monitoringInterval")

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Preferences

    private val prefsX = new Prefs (prefsResource)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Daemon

    private object DaemonStatusImpl extends DaemonStatus {
        protected override lazy val jmxObjectName = daemonStatusJmxName
    }

    // - - - - - - - -  - - - - - - - - - --  - - - - -
    // Thread priority changer

    private val threadPriorityChangerX = new ThreadPriorityChanger (prefsX)

    // - - - - - - -  - - - - - - - - - - - - - - - - - -
    // Pools

    private object LowPriorityPoolImpl extends {
        protected override val threads = lowPriorityPoolThreads
        protected override val latencyLimit = lowPriorityPoolLatencyLimit
        protected override val executionLimit = lowPriorityPoolExecutionLimit
        protected override val threadPriorityChanger = threadPriorityChangerX
    } with LowPriorityPool

    private object NormalPriorityPoolImpl extends {
        protected override val threads = normalPriorityPoolThreads
        protected override val latencyLimit = normalPriorityPoolLatencyLimit
        protected override val executionLimit = normalPriorityPoolExecutionLimit
        protected override val threadPriorityChanger = threadPriorityChangerX
    } with NormalPriorityPool

    private object HiPriorityPoolImpl extends {
        protected override val threads = hiPriorityPoolThreads
        protected override val latencyLimit = hiPriorityPoolLatencyLimit
        protected override val executionLimit = hiPriorityPoolExecutionLimit
        protected override val threadPriorityChanger = threadPriorityChangerX
    } with HiPriorityPool

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Scheduler

    private object SchedulerImpl extends {
        protected override val latencyLimit = schedulerLatencyLimit
        protected override val prefs = prefsX
        protected override val threadPriorityChanger = threadPriorityChangerX
    } with Scheduler

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring Actors

    private class MonitoringActorImpl extends {
        protected override val scheduler = SchedulerImpl
        protected override val pool = NormalPriorityPoolImpl
        protected override val interval = monitoringInterval
        protected override val daemonStatus = DaemonStatusImpl
    } with MonitoringActor

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring

    private val monitoringActorsList =
        repeatToList (monitoringActorsCount) {new MonitoringActorImpl}

    private object MonitoringImpl extends {
        protected override val monitoringActors = monitoringActorsList
    } with Monitoring

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actor manager

    private object ActorManagerImpl extends {
        protected override val monitoring = MonitoringImpl
    } with ActorManager

    // - -- -  - - - - - - - - - - - - - - - - - -- - - -
    private object LogDaoImpl extends LogDao

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actors

    private object LogActorImpl extends {
        protected override val scheduler = SchedulerImpl
        protected override val pool = LowPriorityPoolImpl
        protected override val logDao = LogDaoImpl
    } with LogActor

    private object FileActorImpl extends {
        protected override val scheduler = SchedulerImpl
        protected override val pool = NormalPriorityPoolImpl
        protected override val prefs = prefsX
    } with FileActor

    private object DeamonStatusActorImpl extends {
        protected override val scheduler = SchedulerImpl
        protected override val pool = NormalPriorityPoolImpl
        protected override val interval = daemonStatusUpdateInterval
        protected override val daemonStatus = DaemonStatusImpl
        protected override val statusFile = daemonStatusFile
    } with DeamonStatusActor

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Useful addons

    final def startActors (it : Iterable[Actor]) = {
        it.foreach (ActorManagerImpl.startActor (_))
    }

    final def stopActors (it : Iterable[Actor]) = {
        it.foreach (ActorManagerImpl.stopActor (_))
    }

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Init code

    final lazy val start : Unit = {
        // Init logger
        LogServiceAppender.logActor = Some(LogActorImpl)

        // Run actors
        val actors =
            (LogActorImpl
             :: FileActorImpl
             :: DeamonStatusActorImpl
             :: monitoringActorsList)

        startActors (actors)

        // Start scheduling
        SchedulerImpl.start ()
    }
}