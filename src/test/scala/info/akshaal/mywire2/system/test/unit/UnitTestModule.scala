/*
 * TestModule.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.test.unit

import mywire2.Predefs._
import mywire2.system.logger.{LogActor, LogServiceAppender}
import mywire2.system.utils.LowPriorityPool
import mywire2.system.scheduler.Scheduler

object UnitTestModule {
    object LowPriorityPoolImpl extends {
        override val threads = 2
        override val latencyLimit = 1.seconds
        override val executionLimit = 400.milliseconds
    } with LowPriorityPool

    object SchedulerImpl extends {
        override val latencyLimit = 300.microseconds
    } with Scheduler

    object LogActorImpl extends {
        override val scheduler = SchedulerImpl
        override val pool = LowPriorityPoolImpl
    } with LogActor

    LogServiceAppender.logActor = Some(LogActorImpl)
}
