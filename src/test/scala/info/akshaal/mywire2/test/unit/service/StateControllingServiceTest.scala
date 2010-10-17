/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.service

import scala.collection.immutable.{Map => ImmutableMap}

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Operation
import info.akshaal.jacore.test.JacoreSpecWithJUnit
import device._
import device.owfs._
import service.StateControllingService
import strategy.SimpleOnOffStrategy
import domain.{StateUpdated, Temperature}
import utils.ProblemDetector
import utils.stupdate.StateUpdate
import utils.tracker.TemperatureTracker

import unit.UnitTestHelper._

class StateControllingServiceTest extends JacoreSpecWithJUnit ("StateControllingService services specification") {
    import StateControllingServiceTest._

    "StateControllingService" should {
        "work" in {
            withStartedActors [TestStateControllingServiceListener,
                               TestStateControllingService] (
                (listener, service) => {
                    withStartedActor (devices.stateControllingServiceMP.switch) {
                        val started = System.currentTimeMillis

                        listener.waitForMessageAfter {}
                        listener.changes  must beGreaterThan(0)
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must beGreaterThan(1)
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must beGreaterThan(2)
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must beGreaterThan(3)
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must beGreaterThan(4)
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must beGreaterThan(5)
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must beGreaterThan(6)
                        listener.errors  must_==  0

                        listener.waitForMessageAfter {}
                        listener.changes  must beGreaterThan(7)
                        listener.errors  must_==  0

                        devices.stateControllingServiceMP.switch.n must beIn (8 to 24)
                        (listener.ons - listener.offs)  must_!=  0
                        listener.ons  must_!=  0
                        listener.offs  must_!=  0

                        val lasted = System.currentTimeMillis - started
                        lasted  must beIn (180 to 1000)

                        listener.dups  must_!=  0
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
                        service.stateChanges  must_==  1

                        // This is because of silent problem
                        devices.stateControllingServiceMP.switch2
                               .opReadState.runWithFutureAsy.get  must_==  Success(Some(false))

                        // This should trigger 'unavailable temperature' problem
                        Thread.sleep (50.milliseconds.asMilliseconds)
                        service.updateStateIfChangedAsy ()
                        Thread.sleep (10.milliseconds.asMilliseconds)

                        readState  must_==  Success(Some(false))
                        service.problems  must_==  1
                        service.problemGones  must_==  0
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0

                        // This should remove 'unavasilable temperature' problem
                        service.updateTemp (Some(25))
                        Thread.sleep (50.milliseconds.asMilliseconds)
                        readState  must_==  Success(Some(true))
                        service.problems  must_==  1
                        service.problemGones  must_==  1
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0
                        service.stateChanges  must_==  2

                        // This must check temp problem and trigger too many problems case
                        for (i <- 1 to 4) {
                            service.updateTemp (Some(40))
                            Thread.sleep (10.milliseconds.asMilliseconds)
                            readState  must_==  Success(Some(false))
                            service.problems  must_==  1 + i
                            service.problemGones  must_==  1 + i - 1
                            service.tooMany  must_==  0
                            service.tooManyGone  must_==  0

                            service.updateTemp (Some(10))
                            Thread.sleep (10.milliseconds.asMilliseconds)
                            readState  must_==  Success(Some(i != 4))
                            service.problems  must_==  1 + i
                            service.problemGones  must_==  i + 1
                            service.tooManyGone  must_==  0
                        }

                        // Too many problems must be triggered
                        service.tooMany  must_==  1
                        service.tooManyGone  must_==  0

                        service.updateStateIfChangedAsy ()
                        Thread.sleep (50.milliseconds.asMilliseconds)
                        service.tooMany  must_==  1
                        service.tooManyGone  must_==  0
                        readState  must_==  Success(Some(false))

                        service.updateStateIfChangedAsy ()
                        Thread.sleep (10.milliseconds.asMilliseconds)
                        service.tooMany  must_==  1
                        service.tooManyGone  must_==  0
                        readState  must_==  Success(Some(false))

                        // Check expiration of too many problems
                        Thread.sleep (400.milliseconds.asMilliseconds)
                        service.tooMany  must_==  1
                        service.tooManyGone  must_==  1
                        readState  must_==  Success(Some(true))
                    }
                }
            )
        }
    }
}

object StateControllingServiceTest {
    object devices {
        implicit val deviceEnv = injector.getInstanceOf [OwfsDeviceEnv]

        object stateControllingServiceMP extends OwfsMountPoint ("/tmp/mywire") {
            object switch extends DS2405 ("abc") {
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

            object switch2 extends DS2405 ("cde") {
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

        protected def getStateUpdate () : StateUpdate[Boolean] = {
            val st = strategy.getStateUpdate

            // We are going to broadcast set the same value twice.
            if (st.validTime > (60 milliseconds)) {
                new StateUpdate (st.state, st.validTime / 2)
            } else {
                st
            }
        }

        override protected val safeState = false

        override protected val problemDetectors : List[ProblemDetector] = Nil

        override protected val silentProblemDetectors : List[ProblemDetector] = Nil
    }

    class TestStateControllingServiceListener extends TestActor {
        var changes = 0
        var errors = 0
        var ons = 0
        var offs = 0
        var prev : Option[Boolean] = None
        var dups = 0

        subscribe [StateUpdated]

        override def act () = {
            case StateUpdated ("testStateControllingService", value : Boolean) =>
                prev foreach (prevValue => {
                    if (prevValue == value) {
                        if (value) {
                            dups += 1
                        } else {
                            errors += 1
                        }
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
        protected override val trackedTemperatureNames = "temp" :: Nil
        protected override val problemIfUndefinedFor = 50 milliseconds
        protected override val transitionMessages =
            ImmutableMap (true -> "set to true",
                          false -> "set to false")

        var problems = 0
        var problemGones = 0
        var tooMany = 0
        var tooManyGone = 0
        var stateChanges = 0

        override protected def getStateUpdate () : StateUpdate[Boolean] =
                new StateUpdate (state = true, validTime = 1 minutes)

        override protected val safeState = false

        override protected val problemDetectors =
           temperature.problemIf ("temp").greaterThan (30, backOn=15) :: Nil

        def updateTemp (temp : Option[Double]) = {
            this ! new Temperature ("temp", value = temp, average3 = temp)
        }

        override def onProblem (problem : ProblemDetector) = {
            problems += 1
        }

        override def onProblemGone (problem : ProblemDetector) = {
            problemGones += 1
        }

        override protected def onTooManyProblems () {
            tooMany += 1
        }

        override protected def onTooManyProblemsExpired () {
            tooManyGone += 1
        }

        override protected def onNewState (oldState : Option[Boolean], newState : Boolean) {
            super.onNewState (oldState, newState)
            stateChanges += 1
        }
    }
}