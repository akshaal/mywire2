/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit

import scala.collection.mutable.ListBuffer

import com.google.inject.{Guice, Binder}
import java.io.File

import org.specs.mock.MockitoStubs

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv, LowPriorityActorEnv}
import info.akshaal.jacore.module.Module
import info.akshaal.jacore.scheduler.Scheduler
import info.akshaal.jacore.fs.text.TextFile
import info.akshaal.jacore.test.{TestHelper, Waitable}
import info.akshaal.jacore.JacoreManager

import info.akshaal.mywire2.device.owfs.OwfsDeviceEnv

/**
 * Helper methods for convenient testing of actors and stuff depending on actors.
 */
object UnitTestHelper extends TestHelper {
    override val injector = TestModule.injector

    createModuleGraphInDebugDir ("unittest-module.dot")

    /**
     * Basic ancestor for all actor that are to be used in tests.
     */
    class TestActor extends Actor (actorEnv = TestModule.hiPriorityActorEnv) with Waitable

    /**
     * Test module that is used for tests.
     */
    object TestModule extends Module {
        val daemonStatusFileFile = File.createTempFile ("Mywire", "UnitTest")
        daemonStatusFileFile.deleteOnExit

        override lazy val prefsResource = "jacore-unittest.properties"

        override lazy val daemonStatusJmxName = "mywire:name=unitTestStatus" + hashCode
        override lazy val daemonStatusFile = daemonStatusFileFile.getAbsolutePath

        val injector = Guice.createInjector (this)
        val jacoreManager = injector.getInstanceOf [JacoreManager]

        jacoreManager.start

        val hiPriorityActorEnv = injector.getInstanceOf[HiPriorityActorEnv]
        val lowPriorityActorEnv = injector.getInstanceOf[LowPriorityActorEnv]
        val scheduler = injector.getInstanceOf[Scheduler]
        val textFile = injector.getInstanceOf[TextFile]
    }

    object Mocker extends MockitoStubs {
        def newOwfsDeviceEnv : OwfsDeviceEnv = {
            val deviceEnv = mock [OwfsDeviceEnv]
            deviceEnv.actorEnv returns TestModule.hiPriorityActorEnv
            deviceEnv
        }
    }
}
