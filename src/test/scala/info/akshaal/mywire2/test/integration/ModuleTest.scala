/*
 * ModuleTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package integration

import com.google.inject.{Guice, Injector}

import org.testng.annotations.Test
import org.testng.Assert._

import java.lang.management.ManagementFactory
import javax.management.ObjectName

import Predefs._
import system.module.Module
import system.MywireManager
import common.BaseTest

class ModuleTest extends BaseTest {
    TestModule // We use it here

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

object TestModule extends Module {
    override lazy val monitoringInterval = 2.seconds

    override lazy val lowPriorityPoolThreads = 2
    override lazy val lowPriorityPoolLatencyLimit = 1.seconds
    override lazy val lowPriorityPoolExecutionLimit = 500.milliseconds

    override lazy val normalPriorityPoolThreads = 2
    override lazy val normalPriorityPoolLatencyLimit = 100.milliseconds
    override lazy val normalPriorityPoolExecutionLimit = 10.milliseconds

    override lazy val hiPriorityPoolThreads = 1
    override lazy val hiPriorityPoolLatencyLimit = 1.milliseconds
    override lazy val hiPriorityPoolExecutionLimit = 500.microseconds

    override lazy val schedulerLatencyLimit = 4.milliseconds

    override lazy val daemonStatusJmxName = "mywire:name=integrationTestStatus"
    override lazy val daemonStatusUpdateInterval = 5.seconds
    override lazy val daemonStatusFile = "/tmp/mywire2-test.status"

    val injector = Guice.createInjector (this)
    val mywireManager = injector.getInstance (classOf[MywireManager])

    mywireManager.start
}
