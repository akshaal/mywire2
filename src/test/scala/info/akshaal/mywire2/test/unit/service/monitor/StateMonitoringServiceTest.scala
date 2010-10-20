/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.service.monitor

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Operation
import info.akshaal.jacore.test.JacoreSpecWithJUnit
import device._
import device.owfs._
import service.monitor.StateMonitoringService
import domain.StateSensed

import unit.UnitTestHelper._

class StateMonitoringServiceTest extends JacoreSpecWithJUnit ("StateMonitoringService services specification") {
    import StateMonitoringServiceTest._

    "StateMonitoringService" should {
        "work" in {
            withStartedActors [TestStateMonitoringServiceListener,
                               TestStateMonitoringService] (
                (listener, service) => {
                    withStartedActor (devices.stateMonitoringServiceMP.switch) {
                        val started = System.currentTimeMillis
                        listener.state must_==  None
                        listener.recs  must_==  0
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.state must_==  Some(true)
                        listener.recs  must_==  1
                        listener.errors  must_==  0

                        // It is important that the same value received twice
                        // Because programs like jrobin wants to have value for every
                        // point in time.
                        listener.waitForMessageAfter {}
                        listener.state must_==  Some(false)
                        listener.recs  must_==  2
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.state must_==  Some(false)
                        listener.recs  must_==  3
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.state must_==  None
                        listener.recs  must_==  4
                        listener.errors  must_==  0

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (1200 to 4200)
                    }
                }
            )
        }
    }
}

object StateMonitoringServiceTest {
    object devices {
        implicit val deviceEnv = injector.getInstanceOf [OwfsDeviceEnv]

        object stateMonitoringServiceMP extends OwfsMountPoint ("/tmp/mywire") {
            object switch extends DS2405 ("abc") {
                var n = 0

                override def opGetStateFromFile (file : String) : Operation.WithResult [Boolean] =
                    new AbstractOperation [Result[Boolean]] {
                        override def processRequest () = {
                            val result : Result[Boolean] = n match {
                                case 0 => Success (true)
                                case 1 => Success (false)
                                case 2 => Success (false)
                                case _ => Failure ("ff", Some(new RuntimeException ()))
                            }
                            n += 1

                            yieldResult (result)
                        }
                    }
            }
        }
    }

    class TestStateMonitoringServiceListener extends TestActor {
        var state : Option[Boolean] = None
        var recs = 0
        var errors = 0

        subscribe [StateSensed]

        override def act () = {
            case StateSensed ("testStateMonitoringService", None) =>
                state = None
                recs += 1

            case StateSensed ("testStateMonitoringService", Some(value : Boolean)) =>
                state = Some (value)
                recs += 1

            case StateSensed ("testStateMonitoringService", _) =>
                errors += 1
        }
    }

    class TestStateMonitoringService
             extends StateMonitoringService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateMonitoringServiceMP.switch.Sensed,
                                serviceName = "testStateMonitoringService",
                                interval = 1 seconds)
}