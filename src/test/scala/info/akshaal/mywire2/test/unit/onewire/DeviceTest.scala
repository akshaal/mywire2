/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.onewire

import scala.collection.mutable.ListBuffer

import org.specs.SpecificationWithJUnit
import org.specs.mock.Mockito

import java.io.File

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.fs.text.TextFile
import info.akshaal.jacore.actor.Operation
import onewire.device._

import unit.UnitTestHelper._

class DeviceTest extends SpecificationWithJUnit ("1-wire devices specification") with Mockito {
    import DeviceTest._

    "DS18S20" should {
        "work" in {
            val fnf = new java.io.FileNotFoundException ("not found")
            val readFs = Map ("/tmp/test/uncached/10.abc/temperature" -> Success("23.44"),
                              "/tmp/test/uncached/10.bca/temperature" -> Failure[String](fnf))
            
            withMockedTextFile (readFs) (textFileActor => {
                val deviceEnv = Mocker.newDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new MountPoint ("/tmp/test") {
                    object temp1 extends DS18S20 ("abc", deviceEnv)
                    object temp2 extends DS18S20 ("bca", deviceEnv)
                }

                withStartedActor (mp.temp1) {
                    runOneOperation (mp.temp1.readTemperature ())  must_==  Success (23.44)
                }

                withStartedActor (mp.temp2) {
                    runOneOperation (mp.temp2.readTemperature ())  must_==  Failure[String](fnf)
                }
            })
        }
    }

    def runOneOperation [A] (runner : => Operation.WithComplexResult [A]) : A =
    {
        class RunnerActor extends TestActor {
            val buf = new ListBuffer [A]

            def run () : Unit = {
                postponed ("run") {
                    runner matchResult (buf += _)
                }
            }
        }

        val runnerActor = new RunnerActor

        withStartedActor (runnerActor) {
            waitForMessageBatchesAfter (runnerActor, 2) {runnerActor.run}
        }

        runnerActor.buf (0)
    }

    def withMockedTextFile (reader : String => Result[String])
                           (textFileUser : TextFile => Unit) : Unit =
    {
        val textFile = new TestTextFileActor (reader)

        textFile.start
        try {
            textFileUser (textFile)
        } finally {
            textFile.stop
        }
    }
}

object DeviceTest {
    /**
     * Mocked text file reader.
     */
    class TestTextFileActor (read : String => Result[String]) extends TestActor with TextFile
    {
        override def writeFile (file : File, content : String, payload : Any) : Unit =
        {
            throw new RuntimeException ("NYI")
        }

        def writeFile (file : File, content : String) : Operation.WithResult [Unit] = {
            operation [Unit] ("writeFile") (resultReceiver => throw new RuntimeException ("NYI"))
        }

        override def readFile (file : File, payload : Any) : Unit =
        {
            throw new RuntimeException ("NYI")
        }

        override def readFile (file : File) : Operation.WithResult [String] = {
            operation [String] ("readFile") (resultReceiver =>
                    resultReceiver (read (file.getPath))
                )
        }
    }
}