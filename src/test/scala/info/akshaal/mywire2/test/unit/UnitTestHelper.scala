/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit

import scala.collection.mutable.HashMap

import com.google.inject.{Guice, Binder}
import java.io.File

import org.specs.mock.MockitoStubs

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv, LowPriorityActorEnv, Operation}
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

    /**
     * Basic ancestor for all actor that are to be used in tests.
     */
    class TestActor extends Actor (actorEnv = TestModule.hiPriorityActorEnv) with Waitable

    /**
     * Provides a way to create mocked objects in tests.
     */
    object Mocker extends MockitoStubs {
        def newOwfsDeviceEnv : OwfsDeviceEnv = {
            val deviceEnv = mock [OwfsDeviceEnv]
            deviceEnv.actorEnv returns TestModule.hiPriorityActorEnv
            deviceEnv
        }
    }
    
    /**
     * Execute code with mocked test file actor.
     */
    def withMockedTextFile (reader : String => Result[String] = Map(),
                            writer : (String, String) => Unit = (new HashMap).update)
                           (code : TextFile => Unit) : Unit =
    {
        val textFile = new TestTextFileActor (reader, writer)

        textFile.start
        try {
            code (textFile)
        } finally {
            textFile.stop
        }
    }

    /**
     * Mocked text file reader/writer.
     */
    class TestTextFileActor (read : String => Result[String],
                             writer : (String, String) => Unit)
                    extends TestActor with TextFile
    {
        override def writeFileAsy (file : File, content : String, payload : Any) : Unit =
        {
            throw new RuntimeException ("NYI")
        }

        override def opWriteFile (file : File, content : String) : Operation.WithResult [Unit] = {
            new AbstractOperation [Result [Unit]] {
                override def processRequest () {
                    writer (file.getPath, content)
                    yieldResult (null)
                }
            }
        }

        override def readFileAsy (file : File, payload : Any, size : Option[Int] = None) : Unit =
        {
            throw new RuntimeException ("NYI")
        }

        override def opReadFile (file : File, size : Option[Int] = None) : Operation.WithResult [String] = {
            new AbstractOperation [Result[String]] {
                override def processRequest () {
                    yieldResult (read (file.getPath))
                }
            }
        }
    }
}
