/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service

import scala.collection.immutable.{Map => ImmutableMap}

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}
import info.akshaal.jacore.scheduler.ScheduleControl

import domain.StateUpdated
import utils.{AbstractStateUpdate, StateUpdate, StateUpdateScript, Problem}
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
    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Private state

    private var earlyUpdateControl : Option[ScheduleControl] = None
    private var previousState : Option[T] = None
    private var currentProblem : Option[Problem] = None
    private var problemEndHistory : List[TimeValue] = Nil
    private var disabled = false

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Customization of behavior

    /**
     * List of possible problems.
     */
    protected val possibleProblems : List [Problem] = Nil

    /**
     * List of possible problem that don't generate any error messages, but just
     * switches device to the safe mode.
     */
    protected val possibleSilentProblems : List [Problem] = Nil

    /**
     * Get new state.
     */
    protected def getStateUpdate () : AbstractStateUpdate[T]

    /**
     * Returns state that is considered safe for human environment.
     */
    protected val safeState : T

    /**
     * Messages to be logged when a state is changed to some new state.
     * Value must be mapping from the new state to a message to log.
     */
    protected val transitionMessages : ImmutableMap [T, String] = ImmutableMap ()

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Public interface

    /**
     * Update state if state is changed.
     */
    def updateStateIfChangedAsy () : Unit = {
        postponed {
            updateState (onlyIfChanged = true)
        }
    }

    /**
     * Override method from Actor class. This is used to update state immediately
     * after actor is started.
     */
    override def start () : Boolean = {
        val started = super.start ()

        if (started) {
            updateStateIfChangedAsy ()
        }

        started
    }

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Reactions on events on actions

    /**
     * Method from AbstractControllingService. This method is called, when a
     * tracked message received. This method is used to initiate update process
     * for the state.
     */
    protected override def onTrackedMessageHandled () : Unit = {
        updateStateIfChangedAsy ()
    }

    /**
     * Called when a problem detected.
     */
    protected def onProblem (problem : Problem) : Unit = {
        businessLogicProblem (name +:+ "Problem detected" +:+ problem.detected.get)
    }

    /**
     * Called when a problem is gone.
     */
    protected def onProblemGone (problem : Problem) : Unit = {
        businessLogicInfo (name +:+ "Problem gone" +:+ problem.isGone.get)
    }

    /**
     * Called when too many problems detected.
     */
    protected def onTooManyProblems () : Unit = {
        businessLogicProblem (name +:+ "Too many problems occured within last "
                              + tooManyProblemsInterval
                              + ". Service will be switched into safe mode for the next "
                              + disableOnTooManyProblemsFor)
    }

    /**
     * Called when service is switched back online after too many problems.
     */
    protected def onTooManyProblemsExpired () : Unit = {
        businessLogicInfo (name +:+ "Service is back online after too many problems expired")
    }

    /**
     * Called when state is changed (updated). Default implementation
     * logs message defined in transitionMessages map.
     */
    protected def onNewState (oldState : Option[T], newState : T) : Unit = {
        for (msg <- transitionMessages.get (newState)) {
            businessLogicInfo (msg)
        }
    }

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Internals

    schedule every interval executionOf updateState()

    /**
     * Cancels early update. Early update is update that is scheduled to occur before
     * regular update occurs.
     *
     * For example, if regular updates occur with interval of 5 seconds. Then an early
     * update can be scheduled to occur in 1 second (that is before regular update).
     * And if actual update will happen even before early update, then early update
     * must be canceled. Also, this method is supposed to be called before any update
     * is processed.
     *
     * If early update is already cancelled or not scheduled at all, this method does nothing.
     */
    private def cancelEarlyUpdate () : Unit = {
        earlyUpdateControl foreach (_.cancel ())
        earlyUpdateControl = None
    }

    /**
     * Disable updates for the given period of time.
     */
    private def disable (period : TimeValue, onEnable : => Unit) : Unit = {
        if (disabled) {
            error ("The service is already disabled!")
        }

        // Remember that we are disabled
        disabled = true

        // Schedule some code to enable this service again after period is ended
        schedule in period executionOf {
            // Notify about end of disabled period
            onEnable

            // Reset flag
            disabled = false

            // Set new state
            updateState()
        }
    }

    /**
     * Checks if previously detected problem has disappeared.
     *
     * This method does nothing if no problem is previously detected.
     */
    private def detectProblemDisappearance () : Unit = {
        if (disabled) {
            return
        }

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

                    disable (disableOnTooManyProblemsFor,
                             onEnable = onTooManyProblemsExpired ())
                }
            }
        }
    }

    /**
     * Detect problems.
     *
     * This method does nothing if a problem is already detected.
     */
    private def detectProblem () : Unit = {
        if (disabled) {
            return
        }

        if (currentProblem == None) {
            for (newProblem <- (commonPossibleProblems ++ possibleProblems).find (_.detected != None)) {
                onProblem (newProblem)
                currentProblem = Some (newProblem)
            }
        }
    }

    /**
     * Find silent problem.
     */
    private def findSilentProblem : Option[Problem] = {
        val allPossibleSilentProblems = commonPossibleSilentProblems ++ possibleSilentProblems
        val silentProblem = allPossibleSilentProblems.find (_.detected != None)

        if (isDebugEnabled) {
            for (silentProblem <- silentProblem) {
                debug ("Found silent problem" +:+ silentProblem.detected)
            }
        }

        silentProblem
    }

    /**
     * Update state.
     */
    private def updateState (onlyIfChanged : Boolean = false) : Unit = {
        cancelEarlyUpdate ()
        detectProblemDisappearance ()
        detectProblem ()
        
        // Check for silent problems
        lazy val foundSilentProblems = findSilentProblem

        // Depending on the detected problems, update state
        if (currentProblem == None && !disabled && foundSilentProblems == None) {
            // Everything is good, continuing with update
            getStateUpdate() match {
                case scriptStateUpdate : StateUpdateScript[_] =>
                    // Request to run script
                    doUpdateUsing (scriptStateUpdate, onlyIfChanged)

                case stateUpdate : StateUpdate[_] =>
                    // Request to set some specific state
                    doUpdateUsing (stateUpdate, onlyIfChanged)
            }
        } else {
            doUpdateUsing (new StateUpdate (state = safeState, validTime = interval * 3),
                           onlyIfChanged)
        }
    }

    /**
     * Update state using StateUpdate request.
     */
    private def doUpdateUsing (stateUpdate : StateUpdate[T],
                               onlyIfChanged : Boolean) : Unit =
    {
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
                if (previousState != Some (newState)) {
                    onNewState (previousState, newState)
                }

                previousState = Some (newState)

            case Failure (msg, excOpt) =>
                error ("Error setting state " + newState + " of " + stateContainer
                       +:+ msg +:+ excOpt,
                       excOpt.orNull)
        }

        // Schedule next state update
        earlyUpdateControl =
            if (newValidTime > (0 nanoseconds) && newValidTime < (interval * 2)) {
                Some (schedule in newValidTime executionOf updateState())
            } else {
                None
            }
    }

    /**
     * Update state.
     */
    private def doUpdateUsing (script : StateUpdateScript[T],
                               onlyIfChanged : Boolean) : Unit =
    {
        error ("NOT IMPLEMENTED YET")
    }
}
