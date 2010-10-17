/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service

import scala.collection.immutable.{Map => ImmutableMap}

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.HiPriorityActorEnv
import info.akshaal.jacore.scheduler.ScheduleControl

import domain.StateUpdated
import utils.ProblemDetector
import utils.container.WriteableStateContainer
import utils.stupdate.{AbstractStateUpdate, StateUpdate, StateUpdateScript}

/**
 * A service to control some state.
 * 
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

    // reference to scheduled update due to timeout of currently set state
    private var earlyUpdateControl : Option[ScheduleControl] = None

    // a state we set last time
    private var previousState : Option[T] = None

    // reference to a problem detector that has detected a problem (and the problem is not yet gone)
    private var currentProblemDetector : Option[ProblemDetector] = None

    // each time a problem disappears, a current time is added to the list
    private var problemEndHistory : List[TimeValue] = Nil

    // flag is true when state chaning is disabled due to frequent error or something
    private var disabled = false

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Customization of behavior

    /**
     * List of problem detectors.
     */
    protected val problemDetectors : List [ProblemDetector] = Nil

    /**
     * List of problem detectors that don't generate any error messages, but just
     * switches device to the safe mode.
     */
    protected val silentProblemDetectors : List [ProblemDetector] = Nil

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
     *
     * @return true if started, or false if not
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
     *
     * @param problemDetector problem detector that has detected a problem
     */
    protected def onProblem (problemDetector : ProblemDetector) : Unit = {
        businessLogicProblem (name +:+ "Problem detected" +:+ problemDetector.detected.get)
    }

    /**
     * Called when a problem is gone.
     *
     * @param problemDetector problem detector that detected the problem that is currently gone
     */
    protected def onProblemGone (problemDetector : ProblemDetector) : Unit = {
        businessLogicInfo (name +:+ "Problem gone" +:+ problemDetector.isGone.get)
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
     *
     * @param oldState old state (or None if state is changed first time)
     * @param newState new state
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
     *
     * @param period disable any updates for the given amount of time
     * @param onEnable code to run when it is time to enable updates
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
            updateState ()
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

        for (problemDetector <- currentProblemDetector) {
            // Problem was detected previous time, so just check if it is disappeared
            for (goneMsg <- problemDetector.isGone) {
                onProblemGone (problemDetector)
                currentProblemDetector = None

                // We have to check if too many problems occured within interval
                problemEndHistory ::= System.nanoTime.nanoseconds
                problemEndHistory =
                    problemEndHistory.filter(_ + tooManyProblemsInterval > System.nanoTime.nanoseconds)

                if (problemEndHistory.size >= tooManyProblemsNumber) {
                    onTooManyProblems ()

                    disable (disableOnTooManyProblemsFor, onEnable = onTooManyProblemsExpired ())
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

        if (currentProblemDetector.isEmpty) {
            val allProblemDetectors = basicProblemDetectors ++ problemDetectors
            
            for (newProblemDetector <- allProblemDetectors.find (_.detected.isDefined)) {
                onProblem (newProblemDetector)
                currentProblemDetector = Some (newProblemDetector)
            }
        }
    }

    /**
     * Find silent problem.
     *
     * @return problem detector that detected a silent problem or None if everything is fine
     */
    private def findSilentProblem : Option [ProblemDetector] = {
        val allSilentProblemDetectors = basicSilentProblemDetectors ++ silentProblemDetectors
        val silentProblemDetectorOption = allSilentProblemDetectors.find (_.detected.isDefined)

        if (isDebugEnabled) {
            for (silentProblemDetector <- silentProblemDetectorOption) {
                debug ("Found silent problem" +:+ silentProblemDetector.detected)
            }
        }

        silentProblemDetectorOption
    }

    /**
     * Find out new state, check for problems and may be update state.
     *
     * @param onlyIfChanged if true, don't update underlaying state controller when new state
     *        is the same as current.
     */
    private def updateState (onlyIfChanged : Boolean = false) : Unit = {
        cancelEarlyUpdate ()
        detectProblemDisappearance ()
        detectProblem ()
        
        // Check for silent problems
        lazy val foundSilentProblems = findSilentProblem

        // Depending on the detected problems, update state
        if (currentProblemDetector == None && !disabled && foundSilentProblems.isEmpty) {
            // Everything is good, continuing with update
            getStateUpdate () match {
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
     * Update state using the given state update object.
     *
     * @param stateUpdate object to be used to update state
     * @param onlyIfchanged if true, don't update underlaying state controller when new state
     *        is the same as current.
     */
    private def doUpdateUsing (stateUpdate : StateUpdate[T],
                               onlyIfChanged : Boolean) : Unit =
    {
        val newState = stateUpdate.state
        val newValidTime = stateUpdate.validTime

        // Schedule next state update, we must do this, because we probably canceled previous one
        if (earlyUpdateControl.isEmpty) {
            earlyUpdateControl =
                if (newValidTime > (0 nanoseconds) && newValidTime < (interval * 2)) {
                    Some (schedule in newValidTime executionOf updateState())
                } else {
                    None
                }
        }

        // Update only if changed or onlyIfChanged flag is false
        if (!onlyIfChanged || Some (newState) != previousState) {
            // Change state
            setStateAsy (newState) (_ => ())
        }
    }

    /**
     * Set state asynchronously.
     *
     * @param newState state to set in the underlying state container
     * @param additionalHandler a handler to be invoked when set operation is over
     */
    private def setStateAsy (newState : T)
                            (additionalHandler : Result[Unit] => Unit) : Unit =
    {
        stateContainer.opSetState (newState) runMatchingResultAsy {
            case result @ Success (_) =>
                val stateUpdated = new StateUpdated (name = name, value = newState)
                broadcaster.broadcast (stateUpdated)

                val newStateOption = Some (newState)
                if (previousState != newStateOption) {
                    onNewState (previousState, newState)
                }

                previousState = newStateOption

                additionalHandler (result)

            case result @ Failure (msg, excOption) =>
                error ("Error setting state " + newState + " of " + stateContainer
                       +:+ msg +:+ excOption,
                       excOption.orNull)
                
                additionalHandler (result)
        }
    }

    /**
     * Update state using script.
     *
     * @param script script to be used to update state
     * @param onlyIfChanged if true, don't update underlaying state controller when new state
     *        is the same as current.
     */
    private def doUpdateUsing (script : StateUpdateScript[T],
                               onlyIfChanged : Boolean) : Unit =
    {
        error ("NOT IMPLEMENTED YET")
    }
}
