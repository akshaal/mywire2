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
import strategy.SimpleOnOffStrategy
import domain.{Temperature, Humidity, StateUpdated}
import utils.StateUpdate

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

    "HumidityMonitoringService" should {
        "work" in {
            withStartedActors [TestHumidityMonitoringServiceListener,
                               TestHumidityMonitoringService] (
                (listener, service) => {
                    withStartedActor (devices.humidityMonitoringServiceMP.hum) {
                        val started = System.currentTimeMillis

                        listener.waitForMessageAfter {}
                        listener.hum  must_==  70.1
                        listener.recs  must_==  1

                        listener.waitForMessageAfter {}
                        listener.hum  must_==  60.2
                        listener.recs  must_==  2

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (600 to 2100)
                    }
                }
            )
        }
    }

    "StateControllingService" should {
        "work" in {
            withStartedActors [TestStateControllingServiceListener,
                               TestStateControllingService] (
                (listener, service) => {
                    withStartedActor (devices.stateControllingServiceMP.switch) {
                        val started = System.currentTimeMillis

                        listener.waitForMessageAfter {}
                        listener.changes  must_==  1
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must_==  2
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must_==  3
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must_==  4
                        listener.errors  must_==  0

                        devices.stateControllingServiceMP.switch.n must beIn (4 to 12)
                        (listener.ons - listener.offs)  must_==  0

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (200 to 1000)
                    }
                }
            )
        }
    }
}

object ServiceTest {
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

        object humidityMonitoringServiceMP extends MountPoint ("/tmp/mywire") {
            object hum extends DS2438 ("abc", deviceEnv) with HIH4000 {
                var n = 0

                override def opReadHumidity () : Operation.WithResult [Double] =
                    new AbstractOperation [Result[Double]] {
                        override def processRequest () = {
                            val result : Result[Double] = n match {
                                case 0 => Success (70.1)
                                case 1 => Success (60.2)
                                case _ => Failure (new RuntimeException ())
                            }
                            n += 1

                            yieldResult (result)
                        }
                    }
            }
        }

        object stateControllingServiceMP extends MountPoint ("/tmp/mywire") {
            object switch extends DS2405 ("abc", deviceEnv) {
                var n = 0

                override def opSetState (state : Boolean) : Operation.WithResult [Unit] = {
                    new AbstractOperation [Result[Unit]] {
                        override def processRequest () = {
                            n += 1
                            yieldResult (Success(null))
                        }
                    }
                }
            }
        }
    }

    // Temperature testing - - - - - - - - - -
    
    class TestTemperatureMonitoringServiceListener extends TestActor {
        var temp : Double = 0.0
        var recs = 0

        subscribe [Temperature]

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

    // Humidity testing - - - - - - - - - -

    class TestHumidityMonitoringServiceListener extends TestActor {
        var hum : Double = 0.0
        var recs = 0

        subscribe [Humidity]

        override def act () = {
            case Humidity ("testHumidityMonitoringService", value) =>
                hum = value
                recs += 1
        }
    }

    class TestHumidityMonitoringService
             extends HumidityMonitoringService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                humidityDevice = devices.humidityMonitoringServiceMP.hum,
                                name = "testHumidityMonitoringService",
                                interval = 1 seconds)

    // StateControllingService testing - - - - - - - - - -

    class TestStateControllingService
            extends StateControllingService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.switch,
                                name = "testStateControllingService",
                                interval = 170 seconds)
    {
        val strategy = new SimpleOnOffStrategy (onInterval = 100 milliseconds,
                                                offInterval = 40 milliseconds)

        protected def getStateUpdate () : StateUpdate[Boolean] = strategy.getStateUpdate
    }

    class TestStateControllingServiceListener extends TestActor {
        var changes = 0
        var errors = 0
        var ons = 0
        var offs = 0
        var prev : Option[Boolean] = None

        subscribe [StateUpdated]

        override def act () = {
            case StateUpdated ("testStateControllingService", value : Boolean) =>
                prev foreach (prevValue => {
                    if (prevValue == value) {
                        errors += 1
                    }
                })

                if (value) {
                    ons += 1
                } else {
                    offs += 1
                }
                
                changes += 1

                prev = Some (value)
        }
    }
}
