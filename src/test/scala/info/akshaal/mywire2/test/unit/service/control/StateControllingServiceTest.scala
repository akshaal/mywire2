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
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        "work" in {
            withStartedActors [WorkTestStateControllingServiceListener,
                               WorkTestStateControllingService] (
                (listener, service) => {
                    withStartedActor (mp.workSwitch) {
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

                        mp.workSwitch.n must beIn (8 to 24)
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

        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        "be able to run scripts" in {
            withStartedActor [RunScriptTestStateControllingService] (
                service => {
                    withStartedActor (mp.runScriptSwitch) {
                        def readState () = mp.runScriptSwitch.opReadState.runWithFutureAsy.get

                        service.scriptRunning             must_==  0
                        service.scriptEnded               must_==  0
                        service.scriptInterrupted         must_==  0

                        // Run script
                        service.scriptMode = true
                        Thread.sleep (150.milliseconds.asMilliseconds)

                        // At this moment, script should be running
                        service.scriptRunning             must_==  1
                        service.scriptEnded               must_==  0
                        service.scriptInterrupted         must_==  0

                        // Check that wait method works
                        service.scriptStartedWait300      must_==  true
                        service.scriptEndedWait300        must_==  false

                        Thread.sleep (310.milliseconds.asMilliseconds)

                        service.scriptStartedWait300      must_==  false
                        service.scriptEndedWait300        must_==  true

                        // Get rid of script (interrupt it)
                        service.scriptMode = false
                        Thread.sleep (150.milliseconds.asMilliseconds)

                        service.scriptRunning             must_==  1
                        service.scriptEnded               must_==  0
                        service.scriptInterrupted         must_==  1

                        Thread.sleep (400.milliseconds.asMilliseconds)

                        service.scriptRunning             must_==  1
                        service.scriptEnded               must_==  0
                        service.scriptInterrupted         must_==  1

                        // Run script again
                        service.scriptMode = true
                        Thread.sleep (150.milliseconds.asMilliseconds)

                        service.scriptRunning     must_==  2
                        service.scriptEnded       must_==  0
                        service.scriptInterrupted must_==  1

                        // Let the script finish
                        Thread.sleep (900.milliseconds.asMilliseconds)
                        service.scriptRunning     must_==  3
                        service.scriptEnded       must_==  1
                        service.scriptInterrupted must_==  1
                    }
                }
            )
        }

        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        "support set method in scripts" in {
            withStartedActor [SetStateScriptTestStateControllingService] (
                service => {
                    withStartedActor (mp.setStateScriptSwitch) {
                        def readState () = mp.setStateScriptSwitch.opReadState.runWithFutureAsy.get

                        Thread.sleep (50.milliseconds.asMilliseconds)

                        service.scriptRunning             must_==  1
                        service.scriptEnded               must_==  0
                        service.scriptInterrupted         must_==  0
                        readState ()                      must_==  Success(None)

                        // in 100 milliseconds we must be in the middle of wait(100)
                        // but state must be already true
                        Thread.sleep (100.milliseconds.asMilliseconds)
                        service.scriptRunning             must_==  1
                        service.scriptEnded               must_==  0
                        service.scriptInterrupted         must_==  0
                        readState ()                      must_==  Success(Some (true))

                        // in next 100 milliseconds we must be in the middle of next wait(100).
                        // state must be already false
                        Thread.sleep (100.milliseconds.asMilliseconds)
                        service.scriptRunning             must_==  1
                        service.scriptEnded               must_==  0
                        service.scriptInterrupted         must_==  0
                        readState ()                      must_==  Success(Some (false))

                        // Let the script finish
                        Thread.sleep (100.milliseconds.asMilliseconds)
                        service.scriptRunning     must_==  2
                        service.scriptEnded       must_==  1
                        service.scriptInterrupted must_==  0
                    }
                }
            )
        }

        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        // - - - - - - - - - - - - - - - - - - - - - - - -  - - - - - - - --  --  -- -  - - -
        "be safe" in {
            withStartedActor [BeSafeTestStateControllingService] (
                service => {
                    withStartedActor (mp.beSafeSwitch) {
                        def readState () = mp.beSafeSwitch.opReadState.runWithFutureAsy.get
                        
                        Thread.sleep (50.milliseconds.asMilliseconds)
                        service.problems  must_==  0
                        service.problemGones  must_==  0
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0
                        service.stateChanges  must_==  1 // service changes it at startup

                        // This is because of silent problem
                        readState ()  must_==  Success (Some(false))

                        // This should trigger 'unavailable temperature' problem
                        Thread.sleep (50.milliseconds.asMilliseconds)
                        service.updateStateIfChangedAsy ()
                        Thread.sleep (30.milliseconds.asMilliseconds)

                        readState  must_==  Success (Some (false))
                        service.problems  must_==  1
                        service.problemGones  must_==  0
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0

                        // This should remove 'unavasilable temperature' problem
                        service.updateTemp (Some(25))
                        Thread.sleep (50.milliseconds.asMilliseconds)

                        readState  must_==  Success (Some (true))
                        service.problems  must_==  1
                        service.problemGones  must_==  1
                        service.tooMany  must_==  0
                        service.tooManyGone  must_==  0
                        service.stateChanges  must_==  2

                        // This must check temp problem and trigger too many problems case
                        for (i <- 1 to 4) {
                            service.updateTemp (Some(40))
                            Thread.sleep (50.milliseconds.asMilliseconds)
                            readState  must_==  Success(Some(false))
                            service.problems  must_==  1 + i
                            service.problemGones  must_==  1 + i - 1
                            service.tooMany  must_==  0
                            service.tooManyGone  must_==  0

                            service.updateTemp (Some(10))
                            Thread.sleep (50.milliseconds.asMilliseconds)
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
                        Thread.sleep (40.milliseconds.asMilliseconds)
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

// ------------------------------------------------------------------------
// ------------------------------------------------------------------------
// ------------------------------------------------------------------------
// Stuff used in tests

object StateControllingServiceTest {
    object devices {
        implicit val deviceEnv = injector.getInstanceOf [OwfsDeviceEnv]

        // ------------------------------------------------------------------------
        // ------------------------------------------------------------------------
        // ------------------------------------------------------------------------
        // Mount point for tests

        object stateControllingServiceMP extends OwfsMountPoint ("/tmp/mywire") {
            // ------------------------------------------------------------
            // Switch. Used to test 'work'   - - - - - -  - - - - - - - - -
            object workSwitch extends DS2405 ("abc") {
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

            // ------------------------------------------------------------
            // beSafeSwitch - - -  - - -  - - - -  - - - - - -  - - - - - - - - -
            object beSafeSwitch extends DS2405 ("cde") {
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

            // ----------------------------------------------------------------
            // Used to test scripts - - -  - - - - - - - - -  - - -  -- -  - -
            object runScriptSwitch extends DS2405 ("script1") {
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

            // ----------------------------------------------------------------
            // Used to test scripts with setState - - - - - -  - - -  -- -  - -
            object setStateScriptSwitch extends DS2405 ("stateStateScriptSwitch") {
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

    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // StateControllingService testing: "work" property - - - - - - - - - -

    class WorkTestStateControllingService
            extends StateControllingService [Boolean] (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.workSwitch.PIO,
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

    class WorkTestStateControllingServiceListener extends TestActor {
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

    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // StateControllingService testing "be safe" property - - - - - - - - -

    class BeSafeTestStateControllingService
            extends StateControllingService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.beSafeSwitch.PIO,
                                serviceName = "testStateControllingService",
                                interval = 170 seconds,
                                tooManyProblemsInterval = 550 milliseconds,
                                disableOnTooManyProblemsFor = 300 milliseconds)
    {
        protected override val trackedTemperatureNames = "temp" :: Nil
        protected override val problemIfUndefinedFor = 100 milliseconds
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

    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // StateControllingService testing "run scripts" property - - - - - - -

    class RunScriptTestStateControllingService
            extends StateControllingService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.runScriptSwitch.PIO,
                                serviceName = "RunScriptTestStateControllingService",
                                interval = 50 milliseconds,
                                tooManyProblemsInterval = 300 milliseconds,
                                disableOnTooManyProblemsFor = 300 milliseconds)
    {
        var scriptMode = false
        var scriptRunning = 0
        var scriptEnded = 0
        var scriptInterrupted = 0
        var scriptStartedWait300 = false
        var scriptEndedWait300 = false

        override protected val safeState = false

        override protected def getStateUpdate () =
            if (scriptMode) {
                new StateUpdateScript [Boolean] {
                    protected override def run () : Unit @suspendable = {
                        // On start
                        scriptRunning += 1

                        // Test wait method
                        scriptStartedWait300 = true
                        scriptEndedWait300 = false
                        wait (300 milliseconds)
                        scriptStartedWait300 = false
                        scriptEndedWait300 = true

                        // During this wait, script is supposed to be interrupted
                        wait (400 milliseconds)

                        // On finish, this is unreachable
                        scriptEnded += 1
                    }

                    protected override def defaultOnInterrupt () {
                        scriptInterrupted += 1
                    }
                }
            } else {
                new StateUpdate (state = false, validTime = 1 minutes)
            }
    }

    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // --------------------------------------------------------------------
    // StateControllingService testing "scripts with setState" property - - - - - - -

    class SetStateScriptTestStateControllingService
            extends StateControllingService (
                                actorEnv = TestModule.hiPriorityActorEnv,
                                stateContainer = devices.stateControllingServiceMP.setStateScriptSwitch.PIO,
                                serviceName = "RunScriptTestStateControllingService",
                                interval = 50 milliseconds,
                                tooManyProblemsInterval = 300 milliseconds,
                                disableOnTooManyProblemsFor = 300 milliseconds)
    {
        var scriptRunning = 0
        var scriptEnded = 0
        var scriptInterrupted = 0

        override protected val safeState = false

        override protected def getStateUpdate () =
            new StateUpdateScript [Boolean] {
                protected override def run () : Unit @suspendable = {
                    // On start
                    scriptRunning += 1

                    wait (100 milliseconds)
                    set (true)
                    wait (100 milliseconds)
                    set (false)
                    wait (100 milliseconds)

                    // On finish, this is unreachable
                    scriptEnded += 1
                }

                protected override def defaultOnInterrupt () {
                    scriptInterrupted += 1
                }
            }
    }
}