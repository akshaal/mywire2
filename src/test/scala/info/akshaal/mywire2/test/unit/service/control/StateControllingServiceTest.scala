/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.service.control

import scala.collection.immutable.{Map => ImmutableMap}

import scala.util.continuations._

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Operation
import info.akshaal.jacore.test.JacoreSpecWithJUnit
import device._
import device.owfs._
import service.control.StateControllingService
import strategy.SimpleOnOffStrategy
import domain.{StateUpdated, Temperature}
import utils.ProblemDetector
import utils.stupdate.{StateUpdate, StateUpdateScript}
import utils.tracker.TemperatureTracker

import unit.UnitTestHelper._

class StateControllingServiceTest extends JacoreSpecWithJUnit ("StateControllingService services specification") {
    import StateControllingServiceTest._

    val mp = devices.stateControllingServiceMP

    "StateControllingService" should {
        "work" in {
            withStartedActors [TestStateControllingServiceListener,
                               TestStateControllingService] (
                (listener, service) => {
                    withStartedActor (mp.switch) {
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

                        mp.switch.n must beIn (8 to 24)
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

        "be able to run scripts" in {
            withStartedActor [TestStateControllingServiceScript1] (
                service => {
                    withStartedActor (mp.switch3) {
                        def readState () = mp.switch3.opReadState.runWithFutureAsy.get

                        service.scriptRunning     must_==  false
                        service.scriptEnded       must_==  false
                        service.scriptInterrupted must_==  false

                        // Run script
                        service.scriptMode = true
                        Thread.sleep (150.milliseconds.asMilliseconds)

                        // At this moment, script should be running
                        service.scriptRunning     must_==  true
                        service.scriptEnded       must_==  false
                        service.scriptInterrupted must_==  false

                        // Get rid of script (interrupt it)
                        service.scriptMode = false
                        Thread.sleep (150.milliseconds.asMilliseconds)

                        service.scriptRunning     must_==  false
                        service.scriptEnded       must_==  false
                        service.scriptInterrupted must_==  true

                        // Run script again
                        service.scriptMode = true
                        Thread.sleep (150.milliseconds.asMilliseconds)

                        service.scriptRunning     must_==  true
                        service.scriptEnded       must_==  false
                        service.scriptInterrupted must_==  false
                    }
                }
            )
        }

        "be safe" in {
            withStartedActor [TestStateControllingService2] (
                service => {
                    withStartedActor (mp.switch2) {
                        def readState () = mp.switch2.opReadState.runWithFutureAsy.get

                        Thread.sleep (10.milliseconds.asMilliseconds)
                        service.problems  must_==  0
                        service.problemGones  must_==  0
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0
                        service.stateChanges  must_==  1

                        // This is because of silent problem
                        mp.switch2
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

        // ------------------------------------------------------------------------
        // Mount point for tests

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
                            yieldResult (Success (null))
                        }
                    }
                }

                def opReadState () : Operation.WithResult [Option[Boolean]] = {
                    new AbstractOperation [Result[Option[Boolean]]] {
                        override def processRequest () = {
                            yieldResult (Success (state))
                        }
                    }
                }
            }

            // Switch 3. Used to test scripts
            object switch3 extends DS2405 ("script1") {
                var curStateOption : Option[Boolean] = None

                override def opSetStateToFile (file : String, state : Boolean) : Operation.WithResult [Unit] = {
                    new AbstractOperation [Result[Unit]] {
                        override def processRequest () = {
                            if (file == "PIO") {
                                curStateOption = Some (state)
                            }

                            yieldResult (Success (null))
                        }
                    }
                }

                def opReadState () : Operation.WithResult [Option[Boolean]] = {
                    new AbstractOperation [Result[Option[Boolean]]] {
                        override def processRequest () = {
                            yieldResult (Success (curStateOption))
                        }
                    }
                }
            }
        }
    }

    // StateControllingService testing - - - - - - - - - -

    class TestStateControllingService
            extends StateControllingService [Boolean] (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.switch.PIO,
                                serviceName = "testStateControllingService",
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
                                serviceName = "testStateControllingService",
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

    // StateControllingService testing - - - - - - - - - -

    class TestStateControllingServiceScript1
            extends StateControllingService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.switch3.PIO,
                                serviceName = "testStateControllingServiceScript1",
                                interval = 100 milliseconds,
                                tooManyProblemsInterval = 200 milliseconds,
                                disableOnTooManyProblemsFor = 300 milliseconds)
    {
        var scriptMode = false
        var scriptRunning = false
        var scriptEnded = false
        var scriptInterrupted = false

        override protected val safeState = false

        override protected def getStateUpdate () =
            if (scriptMode) {
                new StateUpdateScript [Boolean] {
                    protected override def run () : Unit @suspendable = {
                        scriptRunning = true
                        scriptEnded = false
                        scriptInterrupted = false

                        wait (1 minutes)

                        scriptEnded = true
                        scriptRunning = false
                    }

                    protected override def defaultOnInterrupt () {
                        scriptInterrupted = true
                        scriptRunning = false
                    }
                }
            } else {
                new StateUpdate (state = false, validTime = 1 minutes)
            }
    }
}