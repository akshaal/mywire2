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

    private[system] val prefs = new Prefs (prefsResource)

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
                             threadPriorityChanger = threadPriorityChanger)

    private[system] val normalPriorityPool =
        new NormalPriorityPool (threads = normalPriorityPoolThreads,
                                latencyLimit = normalPriorityPoolLatencyLimit,
                                executionLimit = normalPriorityPoolExecutionLimit,
                                threadPriorityChanger = threadPriorityChanger)

    private[system] val hiPriorityPool =
        new HiPriorityPool (threads = hiPriorityPoolThreads,
                            latencyLimit = hiPriorityPoolLatencyLimit,
                            executionLimit = hiPriorityPoolExecutionLimit,
                            threadPriorityChanger = threadPriorityChanger)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Scheduler

    private[system] val scheduler =
        new Scheduler (latencyLimit = schedulerLatencyLimit,
                       prefs = prefs,
                       threadPriorityChanger = threadPriorityChanger)

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring Actors

    private[system] final class MonitoringActorImpl extends MonitoringActor (
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

    private[system] val logActor = new LogActor (scheduler = scheduler,
                                                 pool = lowPriorityPool,
                                                 logDao = logDao)

    private[system] val fileActor = new FileActor (scheduler = scheduler,
                                                   pool = normalPriorityPool,
                                                   prefs = prefs)

    private[system] val deamonStatusActor = new DeamonStatusActor (
                                       scheduler = scheduler,
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