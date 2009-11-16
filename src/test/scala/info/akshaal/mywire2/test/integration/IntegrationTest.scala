/*
 * ModuleTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package integration

import java.io.File
import com.google.inject.Guice
import org.specs.SpecificationWithJUnit

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.system.daemon.DaemonStatus

import system.module.Module
import system.MywireManager

class IntegrationTest extends SpecificationWithJUnit ("Integration specification") {
    import IntegrationTest._

    IntegrationModule

    "Mywire" should {
        "survive for some time without problems" in {
            try {
                IntegrationModule.mywireManager.start

                Thread.sleep (15.seconds.asMilliseconds)

                IntegrationModule.daemonStatus.isDying         must beFalse
                IntegrationModule.daemonStatus.isShuttingDown  must beFalse
            } finally {
                IntegrationModule.mywireManager.stop
            }
        }
    }
}

object IntegrationTest {
    object IntegrationModule extends Module {
        val daemonStatusFileFile = File.createTempFile ("Mywire2", "IntegrationTest")
        daemonStatusFileFile.deleteOnExit

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

        override lazy val daemonStatusJmxName = "mywire:name=integrationTestStatus" + hashCode
        override lazy val daemonStatusUpdateInterval = 5.seconds
        override lazy val daemonStatusFile = daemonStatusFileFile.getAbsolutePath

        val injector = Guice.createInjector (this)
        val mywireManager = injector.getInstanceOf [MywireManager]
        val daemonStatus = injector.getInstanceOf [DaemonStatus]
    }
}
