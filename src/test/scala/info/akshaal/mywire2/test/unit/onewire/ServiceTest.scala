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
import utils.{StateUpdate, TemperatureTracker, Problem}

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
                        listener.avg3  must_==  36.6
                        listener.recs  must_==  1

                        // It is important that the same value received twice
                        // Because programs like jrobin wants to have value for every
                        // point in time.
                        listener.waitForMessageAfter {}
                        listener.temp  must_==  1.0
                        listener.avg3  must_==  (36.6d + 1.0d) / 2.0d
                        listener.recs  must_==  2

                        listener.waitForMessageAfter {}
                        listener.temp  must_==  1.0
                        listener.avg3  must_==  (36.6d + 1.0d + 1.0d) / 3.0d
                        listener.recs  must_==  3

                        listener.waitForMessageAfter {}
                        listener.temp.isNaN  must_==  true
                        listener.avg3  must_==  (1.0d + 1.0d) / 2.0d
                        listener.recs  must_==  4

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (1800 to 4200)
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
                        listener.avg3  must_==  70.1
                        listener.recs  must_==  1

                        // It is important that the same value received twice
                        // Because programs like jrobin wants to have value for every
                        // point in time.
                        listener.waitForMessageAfter {}
                        listener.hum  must_==  60.2
                        listener.avg3  must_==  (60.2d + 70.1d) / 2.0d
                        listener.recs  must_==  2

                        listener.waitForMessageAfter {}
                        listener.avg3  must_==  (60.2d + 60.2d + 70.1d) / 3.0d
                        listener.hum  must_==  60.2
                        listener.recs  must_==  3

                        listener.waitForMessageAfter {}
                        listener.avg3  must_==  (60.2d + 60.2d) / 2.0d
                        listener.hum.isNaN  must_==  true
                        listener.recs  must_==  4

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (1200 to 4200)
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
                        lasted  must beIn (180 to 1000)
                    }
                }
            )
        }

        "be safe" in {
            withStartedActor [TestStateControllingService2] (
                service => {
                    withStartedActor (devices.stateControllingServiceMP.switch2) {
                        def readState () =
                            devices.stateControllingServiceMP.switch2.opReadState.runWithFutureAsy.get

                        Thread.sleep (10.milliseconds.asMilliseconds)
                        service.problems  must_==  0
                        service.problemGones  must_==  0
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0

                        // This is because of silent problem
                        devices.stateControllingServiceMP.switch2
                               .opReadState.runWithFutureAsy.get  must_==  Success(Some(false))

                        // This should trigger 'unavailable temperature' problem
                        Thread.sleep (50.milliseconds.asMilliseconds)
                        service.updateStateIfChanged ()
                        Thread.sleep (10.milliseconds.asMilliseconds)

                        readState  must_==  Success(Some(false))
                        service.problems  must_==  1
                        service.problemGones  must_==  0
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0

                        // This should remove 'unavasilable temperature' problem
                        service.updateTemp (25)
                        Thread.sleep (50.milliseconds.asMilliseconds)
                        readState  must_==  Success(Some(true))
                        service.problems  must_==  1
                        service.problemGones  must_==  1
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0

                        // This must check temp problem and trigger too many problems case
                        for (i <- 1 to 4) {
                            service.updateTemp (40)
                            Thread.sleep (10.milliseconds.asMilliseconds)
                            readState  must_==  Success(Some(false))
                            service.problems  must_==  1 + i
                            service.problemGones  must_==  1 + i - 1
                            service.tooMany  must_==  0
                            service.tooManyGone  must_==  0

                            service.updateTemp (10)
                            Thread.sleep (10.milliseconds.asMilliseconds)
                            readState  must_==  Success(Some(i != 4))
                            service.problems  must_==  1 + i
                            service.problemGones  must_==  i + 1
                            service.tooManyGone  must_==  0
                        }

                        // Too many problems must be triggered
                        service.tooMany  must_==  1
                        service.tooManyGone  must_==  0

                        service.updateStateIfChanged ()
                        Thread.sleep (50.milliseconds.asMilliseconds)
                        service.tooMany  must_==  1
                        service.tooManyGone  must_==  0
                        readState  must_==  Success(Some(false))

                        service.updateStateIfChanged ()
                        Thread.sleep (10.milliseconds.asMilliseconds)
                        service.tooMany  must_==  1
                        service.tooManyGone  must_==  0
                        readState  must_==  Success(Some(false))

                        // Check expiration of too many problems
                        Thread.sleep (300.milliseconds.asMilliseconds)
                        service.tooMany  must_==  1
                        service.tooManyGone  must_==  1
                        readState  must_==  Success(Some(true))
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
                                case 2 => Success (1.0)
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
                                case 2 => Success (60.2)
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

                override def opSetStateToFile (file : String, state : Boolean) : Operation.WithResult [Unit] = {
                    new AbstractOperation [Result[Unit]] {
                        override def processRequest () = {
                            if (file == "PIO") {
                                n += 1
                            }
                            yieldResult (Success(null))
                        }
                    }
                }
            }

            object switch2 extends DS2405 ("cde", deviceEnv) {
                private var state : Option[Boolean] = None
                var states : List[Boolean] = Nil

                override def opSetStateToFile (file : String, st : Boolean) : Operation.WithResult [Unit] = {
                    new AbstractOperation [Result[Unit]] {
                        override def processRequest () = {
                            if (file == "PIO") {
                                state = Some (st)
                                states = st :: states
                            }
                            yieldResult (Success(null))
                        }
                    }
                }

                def opReadState () : Operation.WithResult [Option[Boolean]] = {
                    new AbstractOperation [Result[Option[Boolean]]] {
                        override def processRequest () = {
                            yieldResult (Success(state))
                        }
                    }
                }
            }
        }
    }

    // Temperature testing - - - - - - - - - -
    
    class TestTemperatureMonitoringServiceListener extends TestActor {
        var temp : Double = 0.0
        var avg3 : Double = 0.0
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
                                temperatureDevice = devices.temperatureMonitoringServiceMP.temp,
                                name = "testTemperatureMonitoringService",
                                interval = 1 seconds,
                                illegalTemperature = Some(85.0))

    // Humidity testing - - - - - - - - - -

    class TestHumidityMonitoringServiceListener extends TestActor {
        var hum : Double = 0.0
        var avg3 : Double = 0.0
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
                                humidityDevice = devices.humidityMonitoringServiceMP.hum,
                                name = "testHumidityMonitoringService",
                                interval = 1 seconds)

    // StateControllingService testing - - - - - - - - - -

    class TestStateControllingService
            extends StateControllingService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.switch.PIO,
                                name = "testStateControllingService",
                                interval = 170 seconds)
    {
        val strategy = new SimpleOnOffStrategy (onInterval = 100 milliseconds,
                                                offInterval = 40 milliseconds)

        protected def getStateUpdate () : StateUpdate[Boolean] = strategy.getStateUpdate

        override protected val safeState = false

        override protected val possibleProblems : List[Problem] = Nil

        override protected val possibleSilentProblems : List[Problem] = Nil
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

    // StateControllingService testing - - - - - - - - - -

    class TestStateControllingService2
            extends StateControllingService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.switch2.PIO,
                                name = "testStateControllingService",
                                interval = 170 seconds,
                                tooManyProblemsInterval = 200 milliseconds,
                                disableOnTooManyProblemsFor = 300 milliseconds)
    {
        private lazy val trackedTemperature = new TemperatureTracker ("temp")
        var problems = 0
        var problemGones = 0
        var tooMany = 0
        var tooManyGone = 0

        override protected def getStateUpdate () : StateUpdate[Boolean] =
                new StateUpdate (state = true, validTime = 1 minutes)

        override protected val safeState = false

        override protected val possibleProblems =
           trackedTemperature.problemIfNaN ::
           trackedTemperature.problemIfUndefinedFor (50 milliseconds) ::
           trackedTemperature.problemIf ("temp").greaterThan (30, backOn=15) :: Nil

        override protected val possibleSilentProblems : List[Problem] =
           trackedTemperature.problemIfUndefined :: Nil

        def updateTemp (temp : Double) = {
            postponed {
                trackedTemperature.updateFrom (new Temperature ("temp", value=temp, average3=temp))
                updateStateIfChanged ()
            }
        }

        override def onProblem (problem : Problem) = {
            problems += 1
        }

        override def onProblemGone (problem : Problem) = {
            problemGones += 1
        }

        override protected def onTooManyProblems () {
            tooMany += 1
        }

        override protected def onTooManyProblemsExpired () {
            tooManyGone += 1
        }
    }
}
