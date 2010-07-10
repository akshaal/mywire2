/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.device.owfs

import scala.collection.mutable.HashMap
import org.specs.mock.Mockito

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import device.owfs._
import unit.UnitTestHelper._

class DS2405Test extends JacoreSpecWithJUnit ("DS2405 devices specification") with Mockito {
    val fnf = new java.io.FileNotFoundException ("not found")

    "DS2405" should {
        "read PIO" in {
            val readFs = Map ("/tmp/test/uncached/05.abc/PIO" -> Success("0"),
                              "/tmp/test/uncached/05.abd/PIO" -> Success("1"),
                              "/tmp/test/uncached/05.abe/PIO" -> Success(""),
                              "/tmp/test/uncached/05.bcd/PIO" -> Failure[String](fnf))

            withMockedTextFile (readFs) (textFileActor => {
                implicit val deviceEnv = Mocker.newOwfsDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new OwfsMountPoint ("/tmp/test") {
                    object dev1 extends DS2405 ("abc")
                    object dev2 extends DS2405 ("abd")
                    object dev3 extends DS2405 ("abe")
                    object dev4 extends DS2405 ("bcd")
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
                implicit val deviceEnv = Mocker.newOwfsDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new OwfsMountPoint ("/tmp/test") {
                    object dev1 extends DS2405 ("abc")
                    object dev2 extends DS2405 ("abd")
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
                implicit val deviceEnv = Mocker.newOwfsDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new OwfsMountPoint ("/tmp/test") {
                    object dev1 extends DS2405 ("abc")
                    object dev2 extends DS2405 ("abd")
                    object dev3 extends DS2405 ("abe")
                    object dev4 extends DS2405 ("bcd")
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
}
