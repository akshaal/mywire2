/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.onewire

import org.specs.SpecificationWithJUnit

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Operation
import onewire.device._
import onewire.service._
import domain.Temperature

import unit.UnitTestHelper._

class ServiceTest extends SpecificationWithJUnit ("1-wire services specification") {
    import ServiceTest._

    "TemperatureMonitoringService" should {
        "work" in {
            withStartedActors [TestTemperatureMonitoringServiceListener,
                               TestTemperatureMonitoringService] (
                (listener, service) => {
                    withStartedActor (devices.temperatureMonitoringServiceMP.temp) {
                        val started = System.currentTimeMillis

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  36.6
                        listener.recs  must_==  1

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  1.0
                        listener.recs  must_==  2

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (600 to 2100)
                    }
                }
            )
        }
    }
}

object ServiceTest {
    class TestTemperatureMonitoringServiceListener extends TestActor {
        var temp : Double = 0.0
        var recs = 0

        broadcaster.subscribe (this, classOf [Temperature])

        override def act () = {
            case Temperature ("testTemperatureMonitoringService", value) =>
                temp = value
                recs += 1
        }
    }

    class TestTemperatureMonitoringService
             extends TemperatureMonitoringService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                temperatureDevice = devices.temperatureMonitoringServiceMP.temp,
                                name = "testTemperatureMonitoringService",
                                interval = 1 seconds)

    object devices {
        val deviceEnv = injector.getInstanceOf [DeviceEnv]

        object temperatureMonitoringServiceMP extends MountPoint ("/tmp/mywire") {
            object temp extends DS18S20 ("abc", deviceEnv) {
                var n = 0

                override def opReadTemperature () : Operation.WithResult [Double] =
                    new AbstractOperation [Result[Double]] {
                        override def processRequest () = {
                            val result : Result[Double] = n match {
                                case 0 => Success (36.6)
                                case 1 => Success (1.0)
                                case _ => Failure (new RuntimeException ())
                            }
                            n += 1

                            yieldResult (result)
                        }
                    }
            }
        }
    }
}
