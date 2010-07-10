/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.service

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Operation
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import device._
import device.owfs._
import service.HumidityMonitoringService
import domain.Humidity

import unit.UnitTestHelper._

class HumidityMonitoringServiceTest extends JacoreSpecWithJUnit ("HumidityMonitoringService services specification") {
    import HumidityMonitoringServiceTest._

    "HumidityMonitoringService" should {
        "work" in {
            withStartedActors [TestHumidityMonitoringServiceListener,
                               TestHumidityMonitoringService] (
                (listener, service) => {
                    withStartedActor (devices.humidityMonitoringServiceMP.hum) {
                        val started = System.currentTimeMillis

                        listener.waitForMessageAfter {}
                        listener.hum  must_==  Some(70.1)
                        listener.avg3  must_==  Some(70.1)
                        listener.recs  must_==  1

                        // It is important that the same value received twice
                        // Because programs like jrobin wants to have value for every
                        // point in time.
                        listener.waitForMessageAfter {}
                        listener.hum  must_==  Some(60.2)
                        listener.avg3  must_==  Some((60.2d + 70.1d) / 2.0d)
                        listener.recs  must_==  2

                        listener.waitForMessageAfter {}
                        listener.avg3  must_==  Some((60.2d + 60.2d + 70.1d) / 3.0d)
                        listener.hum  must_==  Some(60.2)
                        listener.recs  must_==  3

                        listener.waitForMessageAfter {}
                        listener.avg3  must_==  Some((60.2d + 60.2d) / 2.0d)
                        listener.hum  must_==  None
                        listener.recs  must_==  4

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (1200 to 4200)
                    }
                }
            )
        }
    }
}

object HumidityMonitoringServiceTest {
    object devices {
        implicit val deviceEnv = injector.getInstanceOf [OwfsDeviceEnv]

        object humidityMonitoringServiceMP extends OwfsMountPoint ("/tmp/mywire") {
            object hum extends DS2438 ("abc") with HIH4000 {
                var n = 0

                override def opReadHumidity () : Operation.WithResult [Double] =
                    new AbstractOperation [Result[Double]] {
                        override def processRequest () = {
                            val result : Result[Double] = n match {
                                case 0 => Success (70.1)
                                case 1 => Success (60.2)
                                case 2 => Success (60.2)
                                case _ => Failure (new RuntimeException ())
                            }
                            n += 1

                            yieldResult (result)
                        }
                    }
            }
        }
    }

    class TestHumidityMonitoringServiceListener extends TestActor {
        var hum : Option[Double] = Some(0.0)
        var avg3 : Option[Double] = Some(0.0)
        var recs = 0

        subscribe [Humidity]

        override def act () = {
            case Humidity ("testHumidityMonitoringService", value, avg3v) =>
                hum = value
                avg3 = avg3v
                recs += 1
        }
    }

    class TestHumidityMonitoringService
             extends HumidityMonitoringService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                humidityContainer = devices.humidityMonitoringServiceMP.hum,
                                name = "testHumidityMonitoringService",
                                interval = 1 seconds)
}