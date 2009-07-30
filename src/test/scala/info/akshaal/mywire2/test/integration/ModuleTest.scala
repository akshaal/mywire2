/*
 * ModuleTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package integration

import org.testng.annotations.Test
import org.testng.Assert._

import java.lang.management.ManagementFactory
import javax.management.ObjectName

import Predefs._
import system.module.Module
import common.BaseTest

class ModuleTest extends BaseTest {
    TestModule.start

    @Test (groups=Array("integration"), dependsOnGroups=Array("unit"))
    def basic () = {
        val srv = ManagementFactory.getPlatformMBeanServer()
        val statusObj = new ObjectName (TestModule.daemonStatusJmxName)

        // Check status, it must no be dying or shutting down
        assertNotNull (statusObj)
        assertEquals (srv.getAttribute (statusObj, "dying"), false)
        assertEquals (srv.getAttribute (statusObj, "shuttingDown"), false)
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
