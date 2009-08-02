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
    
    val prefs = new Prefs (prefsResource)

    val monitoringInterval = prefs.getTimeUnit("mywire.monitoring.interval")
    val monitoringActorsCount = prefs.getInt("mywire.monitring.actors")

    val lowPriorityPoolThreads = prefs.getInt("mywire.pool.low.threads")
    val lowPriorityPoolLatencyLimit = prefs.getTimeUnit("mywire.pool.low.latency")
    val lowPriorityPoolExecutionLimit = prefs.getTimeUnit("mywire.pool.low.execution")

    val normalPriorityPoolThreads = prefs.getInt("mywire.pool.normal.threads")
    val normalPriorityPoolLatencyLimit = prefs.getTimeUnit("mywire.pool.normal.latency")
    val normalPriorityPoolExecutionLimit = prefs.getTimeUnit("mywire.pool.normal.execution")

    val hiPriorityPoolThreads = prefs.getInt("mywire.pool.hi.threads")
    val hiPriorityPoolLatencyLimit = prefs.getTimeUnit("mywire.pool.hi.latency")
    val hiPriorityPoolExecutionLimit = prefs.getTimeUnit("mywire.pool.hi.execution")

    val schedulerLatencyLimit = prefs.getTimeUnit("mywire.scheduler.latency")

    val daemonStatusJmxName = "mywire:name=status"
    val daemonStatusUpdateInterval = prefs.getTimeUnit("mywire.status.update.interval")
    val daemonStatusFile = prefs.getString("mywire.status.file")

    val fileReadBytesLimit = 1024*1024

    // -- tests

    require (daemonStatusUpdateInterval > monitoringInterval * 2,
             "daemonStatusUpdateInterval must greater than 2*monitoringInterval")

    require (monitoringActorsCount > 0)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Daemon

    private[system] val daemonStatus = new DaemonStatus (daemonStatusJmxName)

    // - - - - - - - -  - - - - - - - - - --  - - - - -
    // Thread priority changer

    private[system] val threadPriorityChanger =
        new ThreadPriorityChanger (prefs)

    // - - - - - - -  - - - - - - - - - - - - - - - - - -
    // Pools

    private[system] val lowPriorityPool =
        new LowPriorityPool (threads = lowPriorityPoolThreads,
                             latencyLimit = lowPriorityPoolLatencyLimit,
                             executionLimit = lowPriorityPoolExecutionLimit,
                             threadPriorityChanger = threadPriorityChanger,
                             prefs = prefs,
                             daemonStatus = daemonStatus)

    private[system] val normalPriorityPool =
        new NormalPriorityPool (threads = normalPriorityPoolThreads,
                                latencyLimit = normalPriorityPoolLatencyLimit,
                                executionLimit = normalPriorityPoolExecutionLimit,
                                threadPriorityChanger = threadPriorityChanger,
                                prefs = prefs,
                                daemonStatus = daemonStatus)

    private[system] val hiPriorityPool =
        new HiPriorityPool (threads = hiPriorityPoolThreads,
                            latencyLimit = hiPriorityPoolLatencyLimit,
                            executionLimit = hiPriorityPoolExecutionLimit,
                            threadPriorityChanger = threadPriorityChanger,
                            prefs = prefs,
                            daemonStatus = daemonStatus)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Scheduler

    private[system] val scheduler =
        new Scheduler (latencyLimit = schedulerLatencyLimit,
                       prefs = prefs,
                       daemonStatus = daemonStatus,
                       threadPriorityChanger = threadPriorityChanger)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring Actors

    private[system] final class MonitoringActorImpl
                            extends MonitoringActor (
                                     scheduler = scheduler,
                                     pool = normalPriorityPool,
                                     interval = monitoringInterval,
                                     daemonStatus = daemonStatus)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring

    private[system] val monitoringActorsList =
        repeatToList (monitoringActorsCount) {new MonitoringActorImpl}

    private[system] val monitoring = new Monitoring (monitoringActorsList)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actor manager

    private[system] val actorManager = new ActorManager (monitoring)

    // - -- -  - - - - - - - - - - - - - - - - - -- - - -
    // DAO

    private[system] val logDao = new LogDao

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actors

    private[system] val logActor =
        new LogActor (scheduler = scheduler,
                      pool = lowPriorityPool,
                      logDao = logDao)

    private[system] val fileActor =
        new FileActor (scheduler = scheduler,
                       pool = normalPriorityPool,
                       readBytesLimit = fileReadBytesLimit,
                       prefs = prefs)

    private[system] val deamonStatusActor =
        new DeamonStatusActor (scheduler = scheduler,
                               pool = normalPriorityPool,
                               interval = daemonStatusUpdateInterval,
                               daemonStatus = daemonStatus,
                               statusFile = daemonStatusFile)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Useful addons

    final def startActors (it : Iterable[Actor]) = {
        it.foreach (actorManager.startActor (_))
    }

    final def stopActors (it : Iterable[Actor]) = {
        it.foreach (actorManager.stopActor (_))
    }

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Init code

    final lazy val start : Unit = {
        // Init logger
        LogServiceAppender.logActor = Some(logActor)

        // Run actors
        val actors =
            (logActor
             :: fileActor
             :: deamonStatusActor
             :: monitoringActorsList)

        startActors (actors)

        // Start scheduling
        scheduler.start ()
    }
}