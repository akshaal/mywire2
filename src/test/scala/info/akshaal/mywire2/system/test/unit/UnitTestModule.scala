/*
 * TestModule.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.test.unit

import mywire2.Predefs._
import mywire2.system.logger.{LogActor, LogServiceAppender}
import mywire2.system.utils.{LowPriorityPool, NormalPriorityPool, HiPriorityPool}
import mywire2.system.scheduler.Scheduler
import mywire2.system.actor.{Monitoring, MonitoringActor, ActorManager, Actor}

abstract class HiPriorityActor extends {
    override val scheduler = UnitTestModule.SchedulerImpl
    override val pool = UnitTestModule.HiPriorityPoolImpl
} with Actor

object UnitTestModule {
    val monitoringInterval = 1.seconds

    // - - - - - - -  - - - - - - - - - - - - - - - - - -
    // Pools

    object LowPriorityPoolImpl extends {
        override val threads = 2
        override val latencyLimit = 1.seconds
        override val executionLimit = 400.milliseconds
    } with LowPriorityPool

    object NormalPriorityPoolImpl extends {
        override val threads = 2
        override val latencyLimit = 400.milliseconds
        override val executionLimit = 100.milliseconds
    } with NormalPriorityPool

    object HiPriorityPoolImpl extends {
        override val threads = 2
        override val latencyLimit = 1.milliseconds
        override val executionLimit = 400.microseconds
    } with HiPriorityPool

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Scheduler

    object SchedulerImpl extends {
        override val latencyLimit = 300.microseconds
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

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Actors

    object LogActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = LowPriorityPoolImpl
    } with LogActor

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Monitoring Actors

    class MonitoringActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = NormalPriorityPoolImpl
        override val interval = monitoringInterval
    } with MonitoringActor

    object MonitoringActor1Impl extends MonitoringActorImpl
    object MonitoringActor2Impl extends MonitoringActorImpl

    // - - - - -- - - - - - - - - - - - - - - - - - - - --
    // Init code

    val actors = List(LogActorImpl) ++ MonitoringImpl.monitoringActors

    LogServiceAppender.logActor = Some(LogActorImpl)
    
    actors.foreach (ActorManagerImpl.startActor (_))
}
