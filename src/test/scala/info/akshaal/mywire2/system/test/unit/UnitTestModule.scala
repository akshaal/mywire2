/*
 * TestModule.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system.test.unit

import Predefs._
import system.actor.Actor
import system.module.Module

object UnitTestModule extends {
    val monitoringInterval = 2.seconds
    val monitoringActorsCount = 2

    val lowPriorityPoolThreads = 2
    val lowPriorityPoolLatencyLimit = 1.seconds
    val lowPriorityPoolExecutionLimit = 500.milliseconds

    val normalPriorityPoolThreads = 2
    val normalPriorityPoolLatencyLimit = 400.milliseconds
    val normalPriorityPoolExecutionLimit = 100.milliseconds

    val hiPriorityPoolThreads = 1
    val hiPriorityPoolLatencyLimit = 40.milliseconds
    val hiPriorityPoolExecutionLimit = 1.milliseconds

    val schedulerLatencyLimit = 10.milliseconds

    override val daemonStatusJmxName = "mywire:name=unitTestDaemonStatus"
    val daemonStatusUpdateInterval = 5.seconds
    val daemonStatusFile = "/tmp/mywire2-unitTest.status"
} with Module {
    start
}

abstract class HiPriorityActor extends Actor (
                     scheduler = UnitTestModule.scheduler,
                     pool = UnitTestModule.hiPriorityPool)