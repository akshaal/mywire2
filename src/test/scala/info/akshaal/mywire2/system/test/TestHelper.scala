/*
 * TestHelper.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.test

import actor.Actor
import system.RuntimeConstants
import scheduler.Scheduler
import utils.{HiPriorityPool, LowPriorityPool}

object TestHelper {
    def startActor (actor : Actor) = actor.start ()

    def exitActor (actor : Actor) = actor.exit ()

    val actorsMonitoringInterval = RuntimeConstants.actorsMonitoringInterval

    def getSchedulerLatencyNano = Scheduler.getLatencyNano

    def getHiPriorityPoolLatency = HiPriorityPool.latency.getNano

    def getLowPriorityPoolLatency = LowPriorityPool.latency.getNano
}
