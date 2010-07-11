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
import domain.Temperature
import service.TemperatureMonitoringService

import unit.UnitTestHelper._

class TemperatureMonitoringServiceTest extends JacoreSpecWithJUnit ("TemperatureMonitoringServiceTest services specification") {
    import TemperatureMonitoringServiceTest._

    "TemperatureMonitoringService" should {
        "work" in {
            withStartedActors [TestTemperatureMonitoringServiceListener,
                               TestTemperatureMonitoringService] (
                (listener, service) => {
                    withStartedActor (devices.temperatureMonitoringServiceMP.temp) {
                        val started = System.currentTimeMillis

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  Some(36.6)
                        listener.avg3  must_==  Some(36.6)
                        listener.recs  must_==  1

                        // It is important that the same value received twice
                        // Because programs like jrobin wants to have value for every
                        // point in time.
                        listener.waitForMessageAfter {}
                        listener.temp  must_==  Some(1.0)
                        listener.avg3  must_==  Some((36.6d + 1.0d) / 2.0d)
                        listener.recs  must_==  2

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  Some(1.0)
                        listener.avg3  must_==  Some((36.6d + 1.0d + 1.0d) / 3.0d)
                        listener.recs  must_==  3

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  Some(2.0)
                        listener.avg3  must_==  Some((1.0d + 1.0d + 2.0d) / 3.0d)
                        listener.recs  must_==  4

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  Some(3.0)
                        listener.avg3  must_==  Some((1.0d + 3.0d + 2.0d) / 3.0d)
                        listener.recs  must_==  5

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  None
                        listener.avg3  must_==  Some((3.0d + 2.0d) / 2.0d)
                        listener.recs  must_==  6

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  Some(4.0)
                        listener.avg3  must_==  Some((4.0d + 3.0d) / 2.0d)
                        listener.recs  must_==  7

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  None
                        listener.avg3  must_==  Some((4.0d) / 1.0d)
                        listener.recs  must_==  8

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (3600 to 8400)
                    }
                }
            )
        }
    }
}

object TemperatureMonitoringServiceTest {
    object devices {
        implicit val deviceEnv = injector.getInstanceOf [OwfsDeviceEnv]

        object temperatureMonitoringServiceMP extends OwfsMountPoint ("/tmp/mywire") {
            object temp extends DS18S20 ("abc") {
                var n = 0

                override def opReadTemperature () : Operation.WithResult [Double] =
                    new AbstractOperation [Result[Double]] {
                        override def processRequest () = {
                            val result : Result[Double] = n match {
                                case 0 => Success (36.6)
                                case 1 => Success (1.0)
                                case 2 => Success (1.0)
                                case 3 => Success (85.0)
                                case 4 => Success (2.0)
                                case 5 => Success (85.0)
                                case 6 => Success (85.0)
                                case 7 => Success (3.0)
                                case 8 => Success (85.0)
                                case 9 => Success (85.0)
                                case 10 => Success (85.0)
                                case 11 => Success (4.0)
                                case _ => Failure ("123", Some(new RuntimeException ()))
                            }
                            n += 1

                            yieldResult (result)
                        }
                    }
            }
        }
    }

    // Temperature testing - - - - - - - - - -

    class TestTemperatureMonitoringServiceListener extends TestActor {
        var temp : Option[Double] = Some(0.0)
        var avg3 : Option[Double] = Some(0.0)
        var recs = 0

        subscribe [Temperature]

        override def act () = {
            case Temperature ("testTemperatureMonitoringService", value, avg3v) =>
                temp = value
                avg3 = avg3v
                recs += 1
        }
    }

    class TestTemperatureMonitoringService
             extends TemperatureMonitoringService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                temperatureContainer = devices.temperatureMonitoringServiceMP.temp,
                                name = "testTemperatureMonitoringService",
                                interval = 1 seconds,
                                illegalTemperature = Some(85.0))
}