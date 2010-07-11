/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}
import info.akshaal.jacore.scheduler.ScheduleControl

import domain.StateUpdated
import utils.{StateUpdate, Problem}
import utils.container.WriteableStateContainer

/**
 * A service to control some state.
 * @param actorEnv actor environment
 * @param stateContainer state container
 * @param name name
 * @param interval interval to check and update state
 * @param tooManyProblemsNumber if number of problems within tooManyProblemsInterval
 *              is greater or equal to this number, then service is disabled (in safe mode)
 * @param tooManyProblemsInterval see tooManyProblemsNumber
 * @param disableOnTooManyProblemsFor if too many problems detected, then the service is disabled
 *                     for this amount of time
 */
abstract class StateControllingService [T] (actorEnv : HiPriorityActorEnv,
                                            stateContainer : WriteableStateContainer [T],
                                            name : String,
                                            interval : TimeValue,
                                            tooManyProblemsNumber : Int = 5,
                                            tooManyProblemsInterval : TimeValue = 10 minutes,
                                            disableOnTooManyProblemsFor : TimeValue = 15 minutes)
                            extends AbstractControllingService (actorEnv = actorEnv)
{
    private var earlyUpdateControl : Option[ScheduleControl] = None
    private var previousState : Option[T] = None
    private var currentProblem : Option[Problem] = None
    private var problemEndHistory : List[TimeValue] = Nil
    private var disabledUntil : Option[TimeValue] = None

    schedule every interval executionOf updateState()

    /**
     * Update state if state is changed.
     */
    def updateStateIfChanged () : Unit = {
        postponed {
            updateState (onlyIfChanged = true)
        }
    }

    protected override def onTrackedMessageHandled () : Unit = {
        updateStateIfChanged ()
    }

    /**
     * Update state.
     */
    private def updateState (onlyIfChanged : Boolean = false) : Unit = {
        earlyUpdateControl foreach (_.cancel ())
        earlyUpdateControl = None

        // Check if we are disabled
        for (time <- disabledUntil) {
            if (System.nanoTime.nanoseconds > time) {
                onTooManyProblemsExpired ()
                disabledUntil = None
            }
        }

        // If not disabled, check for problems
        if (disabledUntil == None) {
            for (problem <- currentProblem) {
                // Problem was detected previous time, so just check if it is disappeared
                for (goneMsg <- problem.isGone) {
                    onProblemGone (problem)
                    currentProblem = None

                    // We have to check if too many problems occured within interval
                    problemEndHistory = System.nanoTime.nanoseconds :: problemEndHistory
                    problemEndHistory = problemEndHistory.filter(_ + tooManyProblemsInterval > System.nanoTime.nanoseconds)
                    if (problemEndHistory.size >= tooManyProblemsNumber) {
                        onTooManyProblems ()
                        disabledUntil = Some (System.nanoTime.nanoseconds + disableOnTooManyProblemsFor)
                        schedule in disableOnTooManyProblemsFor executionOf updateState()
                    }
                }
            }

            if (currentProblem == None) {
                for (newProblem <- (commonPossibleProblems ++ possibleProblems).find (_.detected != None)) {
                    onProblem (newProblem)
                    currentProblem = Some (newProblem)
                }
            }
        }

        // Check for silent problems
        lazy val foundSilentProblems =
            (commonPossibleSilentProblems ++ possibleSilentProblems).find(_.detected != None)

        if (isDebugEnabled) {
            for (silentProblem <- foundSilentProblems) {
                debug ("Found silent problem" +:+ silentProblem.detected)
            }
        }

        // Get new state
        val stateUpdate =
            if (currentProblem == None && disabledUntil == None && foundSilentProblems == None)
                getStateUpdate ()
            else
                new StateUpdate (state = safeState, validTime = interval * 3)

        val newState = stateUpdate.state
        val newValidTime = stateUpdate.validTime

        // Exit from method if onlyIfChanged flag set and state is not changed
        if (onlyIfChanged) {
            for (state <- previousState) {
                if (state == newState) {
                    return
                }
            }
        }

        // Change state
        stateContainer.opSetState (newState) runMatchingResultAsy {
            case Success (_) =>
                val stateUpdated = new StateUpdated (name = name, value = newState)
                broadcaster.broadcast (stateUpdated)

                previousState = Some (newState)

            case Failure (msg, excOpt) =>
                error ("Error setting state " + newState + " of " + stateContainer +:+ msg +:+ excOpt, excOpt.orNull)
        }

        // Schedule next state update
        if (newValidTime > (0 nanoseconds) && newValidTime < (interval * 2)) {
            earlyUpdateControl = Some (schedule in newValidTime executionOf updateState())
        } else {
            earlyUpdateControl = None
        }
    }

    override def start () : Boolean = {
        val started = super.start ()

        if (started) {
            postponed {
                updateState ()
            }
        }

        started
    }

    /**
     * Called when a problem detected.
     */
    protected def onProblem (problem : Problem) {
        businessLogicProblem (name +:+ "Problem detected" +:+ problem.detected.get)
    }

    /**
     * Called when a problem is gone.
     */
    protected def onProblemGone (problem : Problem) {
        businessLogicInfo (name +:+ "Problem gone" +:+ problem.isGone.get)
    }

    /**
     * Called when too many problems detected.
     */
    protected def onTooManyProblems () {
        businessLogicProblem (name +:+ "Too many problems occured within last "
                              + tooManyProblemsInterval
                              + ". Service will be switched into safe mode for the next "
                              + disableOnTooManyProblemsFor)
    }

    /**
     * Called when service is switched back online after too many problems.
     */
    protected def onTooManyProblemsExpired () {
        businessLogicInfo (name +:+ "Service is back online after too many problems expired")
    }

    /**
     * Get new state.
     */
    protected def getStateUpdate () : StateUpdate[T]

    /**
     * Returns state that is considered safe for human environment.
     */
    protected def safeState : T

    /**
     * List of possible problems.
     */
    protected def possibleProblems : List [Problem] = Nil

    /**
     * List of possible problem that don't generate any error messages, but just
     * switches device to the safe mode.
     */
    protected def possibleSilentProblems : List[Problem] = Nil
}
