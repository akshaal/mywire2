/*
 * Module.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.module

import mywire2.Predefs._
import logger.{LogActor, LogServiceAppender}
import utils.{LowPriorityPool, NormalPriorityPool, HiPriorityPool, TimeUnit}
import scheduler.Scheduler
import actor.{Monitoring, MonitoringActor, ActorManager, Actor}
import daemon.DaemonStatus
import fs.FileActor

trait Module {
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

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Daemon

    private object DaemonStatusImpl extends DaemonStatus {
        override lazy val jmxObjectName = daemonStatusJmxName
    }

    // - - - - - - -  - - - - - - - - - - - - - - - - - -
    // Pools

    private object LowPriorityPoolImpl extends {
        override val threads = lowPriorityPoolThreads
        override val latencyLimit = lowPriorityPoolLatencyLimit
        override val executionLimit = lowPriorityPoolExecutionLimit
    } with LowPriorityPool

    private object NormalPriorityPoolImpl extends {
        override val threads = normalPriorityPoolThreads
        override val latencyLimit = normalPriorityPoolLatencyLimit
        override val executionLimit = normalPriorityPoolExecutionLimit
    } with NormalPriorityPool

    private object HiPriorityPoolImpl extends {
        override val threads = hiPriorityPoolThreads
        override val latencyLimit = hiPriorityPoolLatencyLimit
        override val executionLimit = hiPriorityPoolExecutionLimit
    } with HiPriorityPool

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Scheduler

    private object SchedulerImpl extends {
        override val latencyLimit = schedulerLatencyLimit
    } with Scheduler

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring Actors

    private class MonitoringActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = NormalPriorityPoolImpl
        override val interval = monitoringInterval
        override val daemonStatus = DaemonStatusImpl
    } with MonitoringActor

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring

    private object MonitoringImpl extends {
        override val monitoringActors =
            repeatToList (monitoringActorsCount) {new MonitoringActorImpl}
    } with Monitoring

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actor manager

    private object ActorManagerImpl extends {
        override val monitoring = MonitoringImpl
    } with ActorManager

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actors

    private object LogActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = LowPriorityPoolImpl
    } with LogActor

    private object FileActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = NormalPriorityPoolImpl
    } with FileActor

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
             :: MonitoringImpl.monitoringActors)

        startActors (actors)

        // Start scheduling
        SchedulerImpl.start ()
    }
}