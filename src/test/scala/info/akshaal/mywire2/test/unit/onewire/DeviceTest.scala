/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.onewire

import scala.collection.mutable.HashMap

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

    val fnf = new java.io.FileNotFoundException ("not found")

    "DS18S20" should {
        "read temperature" in {
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

    "DS2405" should {
        "read PIO" in {
            val readFs = Map ("/tmp/test/uncached/05.abc/PIO" -> Success("0"),
                              "/tmp/test/uncached/05.abd/PIO" -> Success("1"),
                              "/tmp/test/uncached/05.abe/PIO" -> Success(""),
                              "/tmp/test/uncached/05.bcd/PIO" -> Failure[String](fnf))

            withMockedTextFile (readFs) (textFileActor => {
                val deviceEnv = Mocker.newDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new MountPoint ("/tmp/test") {
                    object dev1 extends DS2405 ("abc", deviceEnv)
                    object dev2 extends DS2405 ("abd", deviceEnv)
                    object dev3 extends DS2405 ("abe", deviceEnv)
                    object dev4 extends DS2405 ("bcd", deviceEnv)
                }

                withStartedActor (mp.dev1) {
                    mp.dev1.PIO.opGetState ().runWithFutureAsy().get  must_==  Success (false)
                }

                withStartedActor (mp.dev2) {
                    mp.dev2.PIO.opGetState ().runWithFutureAsy().get  must_==  Success (true)
                }

                withStartedActor (mp.dev3) {
                    mp.dev3.PIO.opGetState ().runWithFutureAsy().get  must beLike {
                        case Failure(exc) => exc.isInstanceOf[NumberFormatException]
                    }
                }

                withStartedActor (mp.dev4) {
                    mp.dev4.PIO.opGetState ().runWithFutureAsy().get  must_==  Failure[Boolean](fnf)
                }
            })
        }

        "write PIO" in {
            val writeFs = new HashMap[String, String]

            withMockedTextFile (writer = writeFs.update) (textFileActor => {
                val deviceEnv = Mocker.newDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new MountPoint ("/tmp/test") {
                    object dev1 extends DS2405 ("abc", deviceEnv)
                    object dev2 extends DS2405 ("abd", deviceEnv)
                }

                withStartedActor (mp.dev1) {
                    mp.dev1.PIO.opSetState (false).runWithFutureAsy().get
                }

                withStartedActor (mp.dev2) {
                    mp.dev2.PIO.opSetState (true).runWithFutureAsy().get
                }
            })

            writeFs.size must_== 2
            writeFs ("/tmp/test/uncached/05.abc/PIO") must_== "0"
            writeFs ("/tmp/test/uncached/05.abd/PIO") must_== "1"
        }

        "read sensed" in {
            val readFs = Map ("/tmp/test/uncached/05.abc/sensed" -> Success("1"),
                              "/tmp/test/uncached/05.abd/sensed" -> Success("0"),
                              "/tmp/test/uncached/05.abe/sensed" -> Success(""),
                              "/tmp/test/uncached/05.bcd/sensed" -> Failure[String](fnf))

            withMockedTextFile (readFs) (textFileActor => {
                val deviceEnv = Mocker.newDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new MountPoint ("/tmp/test") {
                    object dev1 extends DS2405 ("abc", deviceEnv)
                    object dev2 extends DS2405 ("abd", deviceEnv)
                    object dev3 extends DS2405 ("abe", deviceEnv)
                    object dev4 extends DS2405 ("bcd", deviceEnv)
                }

                withStartedActor (mp.dev1) {
                    mp.dev1.Sensed.opGetState ().runWithFutureAsy().get  must_==  Success (true)
                }

                withStartedActor (mp.dev2) {
                    mp.dev2.Sensed.opGetState ().runWithFutureAsy().get  must_==  Success (false)
                }

                withStartedActor (mp.dev3) {
                    mp.dev3.Sensed.opGetState ().runWithFutureAsy().get  must beLike {
                        case Failure(exc) => exc.isInstanceOf[NumberFormatException]
                    }
                }

                withStartedActor (mp.dev4) {
                    mp.dev4.Sensed.opGetState ().runWithFutureAsy().get  must_==  Failure[Boolean](fnf)
                }
            })
        }
    }

    "DS2438" should {
        "read temperature" in {
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

    def withMockedTextFile (reader : String => Result[String] = Map(),
                            writer : (String, String) => Unit = (new HashMap).update)
                           (textFileUser : TextFile => Unit) : Unit =
    {
        val textFile = new TestTextFileActor (reader, writer)

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
