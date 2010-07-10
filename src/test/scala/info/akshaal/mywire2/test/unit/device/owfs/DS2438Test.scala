/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.device.owfs

import org.specs.mock.Mockito

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import device.owfs._
import unit.UnitTestHelper._

class DS2438Test extends JacoreSpecWithJUnit ("DS2438 devices specification") with Mockito {
    val fnf = new java.io.FileNotFoundException ("not found")

    "DS2438" should {
        "read temperature" in {
            val readFs = Map ("/tmp/test/uncached/26.4445/temperature" -> Success("11.43"),
                              "/tmp/test/uncached/26.3333/temperature" -> Failure[String](fnf))

            withMockedTextFile (readFs) (textFileActor => {
                implicit val deviceEnv = Mocker.newOwfsDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new OwfsMountPoint ("/tmp/test") {
                    object temp1 extends DS2438 ("4445")
                    object temp2 extends DS2438 ("3333")
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
                implicit val deviceEnv = Mocker.newOwfsDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new OwfsMountPoint ("/tmp/test") {
                    object temp1 extends DS2438 ("1445") with HIH4000
                    object temp2 extends DS2438 ("1333") with HIH4000
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
}

