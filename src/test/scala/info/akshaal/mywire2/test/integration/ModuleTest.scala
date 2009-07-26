/*
 * ModuleTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.integration

import org.testng.annotations.Test
import org.testng.Assert._

import mywire2.Predefs._
import system.module.Module
import common.BaseTest

class ModuleTest extends BaseTest {
    TestModule // touch it

    @Test (groups=Array("integration"), dependsOnGroups=Array("unit"))
    def test () = {
        // TODO
    }
}

object TestModule extends {
    override val monitoringInterval = 2.seconds
    override val monitoringActorsCount = 3

    override val lowPriorityPoolThreads = 2
    override val lowPriorityPoolLatencyLimit = 1.seconds
    override val lowPriorityPoolExecutionLimit = 500.milliseconds

    override val normalPriorityPoolThreads = 2
    override val normalPriorityPoolLatencyLimit = 100.milliseconds
    override val normalPriorityPoolExecutionLimit = 10.milliseconds

    override val hiPriorityPoolThreads = 1
    override val hiPriorityPoolLatencyLimit = 1.milliseconds
    override val hiPriorityPoolExecutionLimit = 500.microseconds

    override val schedulerLatencyLimit = 4.milliseconds
} with Module
