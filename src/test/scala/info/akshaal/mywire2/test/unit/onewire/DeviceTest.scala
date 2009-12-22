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
            val readFs = Map ("/tmp/test/uncached/abc/temperature" -> Success("23.44"))
            
            withMockedTextFile (readFs) (textFileActor => {
                val deviceEnv = Mocker.newDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new MountPoint ("/tmp/test") {
                    object temp extends DS18S20 ("abc", deviceEnv)
                }

                mp.temp.start

                try {
                    withStartedActor [DS18S20WorkTest] (actor => {
                        waitForMessageBatchesAfter (actor, 2) {actor.test (mp.temp)}

                        actor.buf  must haveSize (1)

                        val result = actor.buf (0)
                        result  must_==  Success (23.44)
                    })
                } finally {
                    mp.temp.stop
                }
            })
        }
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
    class DS18S20WorkTest extends TestActor {
        val buf = new ListBuffer [Result[Double]]
        
        def test (temp : DS18S20) : Unit = {
            postponed ("test") {
                temp.readTemperature () matchResult (buf += _)
            }
        }
    }

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