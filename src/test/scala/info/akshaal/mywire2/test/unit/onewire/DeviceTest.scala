/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.onewire

import org.specs.SpecificationWithJUnit
import org.specs.mock.Mockito

import java.io.File

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.fs.text.TextFile
import info.akshaal.jacore.actor.Operation
import onewire.device._

import unit.UnitTestHelper._

class DeviceTest extends SpecificationWithJUnit ("1-wire devices specification") with Mockito {
    import DeviceTest._

    "DS18S20" should {
        "read temperature" in {
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
                    mp.temp1.opReadTemperature ().runWithFutureAsy().get  must_==  Success (23.44)
                }

                withStartedActor (mp.temp2) {
                    mp.temp2.opReadTemperature ().runWithFutureAsy().get  must_==  Failure[String](fnf)
                }
            })
        }
    }

    "DS2438" should {
        "read temperature" in {
            val fnf = new java.io.FileNotFoundException ("not found")
            val readFs = Map ("/tmp/test/uncached/26.4445/temperature" -> Success("11.43"),
                              "/tmp/test/uncached/26.3333/temperature" -> Failure[String](fnf))

            withMockedTextFile (readFs) (textFileActor => {
                val deviceEnv = Mocker.newDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new MountPoint ("/tmp/test") {
                    object temp1 extends DS2438 ("4445", deviceEnv)
                    object temp2 extends DS2438 ("3333", deviceEnv)
                }

                withStartedActor (mp.temp1) {
                    mp.temp1.opReadTemperature ().runWithFutureAsy ().get  must_==  Success (11.43)
                }

                withStartedActor (mp.temp2) {
                    mp.temp2.opReadTemperature ().runWithFutureAsy ().get  must_==  Failure[String](fnf)
                }
            })
        }

        "read humidity from HIH4000" in {
            val fnf = new java.io.FileNotFoundException ("not found")
            val readFs = Map ("/tmp/test/uncached/26.1445/HIH4000/humidity"
                                        -> Success("75"),

                              "/tmp/test/uncached/26.1333/HIH4000/humidity"
                                        -> Failure[String] (fnf))

            withMockedTextFile (readFs) (textFileActor => {
                val deviceEnv = Mocker.newDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new MountPoint ("/tmp/test") {
                    object temp1 extends DS2438 ("1445", deviceEnv) with HIH4000
                    object temp2 extends DS2438 ("1333", deviceEnv) with HIH4000
                }

                withStartedActor (mp.temp1) {
                    mp.temp1.opReadHumidity ().runWithFutureAsy ().get  must_==  Success (75.)
                }

                withStartedActor (mp.temp2) {
                    mp.temp2.opReadHumidity ().runWithFutureAsy ().get  must_==  Failure[String](fnf)
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
    /**
     * Mocked text file reader.
     */
    class TestTextFileActor (read : String => Result[String]) extends TestActor with TextFile
    {
        override def writeFileAsy (file : File, content : String, payload : Any) : Unit =
        {
            throw new RuntimeException ("NYI")
        }

        override def opWriteFile (file : File, content : String) : Operation.WithResult [Unit] = {
            new AbstractOperation [Result [Unit]] {
                override def processRequest () {
                    throw new RuntimeException ("NYI")
                }
            }
        }

        override def readFileAsy (file : File, payload : Any) : Unit =
        {
            throw new RuntimeException ("NYI")
        }

        override def opReadFile (file : File) : Operation.WithResult [String] = {
            new AbstractOperation [Result[String]] {
                override def processRequest () {
                    yieldResult (read (file.getPath))
                }
            }
        }
    }
}
