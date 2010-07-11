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

class DS18S20Test extends JacoreSpecWithJUnit ("DS18S20 specification") with Mockito {
    val fnf = new java.io.FileNotFoundException ("not found")

    "DS18S20" should {
        "read temperature" in {
            val readFs = Map ("/tmp/test/uncached/10.abc/temperature" -> Success("23.44"),
                              "/tmp/test/uncached/10.bca/temperature" -> Failure[String]("g", Some(fnf)))

            withMockedTextFile (readFs) (textFileActor => {
                implicit val deviceEnv = Mocker.newOwfsDeviceEnv
                deviceEnv.textFile returns textFileActor

                val mp = new OwfsMountPoint ("/tmp/test") {
                    object temp1 extends DS18S20 ("abc")
                    object temp2 extends DS18S20 ("bca")
                }

                withStartedActor (mp.temp1) {
                    mp.temp1.opReadTemperature ().runWithFutureAsy().get  must_==  Success (23.44)
                }

                withStartedActor (mp.temp2) {
                    mp.temp2.opReadTemperature ().runWithFutureAsy().get  must_==  Failure[String]("g", Some(fnf))
                }
            })
        }
    }
}
