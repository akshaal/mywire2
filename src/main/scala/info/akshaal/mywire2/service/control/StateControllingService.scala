/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service.control

import scala.collection.immutable.{Map => ImmutableMap}

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.HiPriorityActorEnv
import info.akshaal.jacore.scheduler.ScheduleControl

import domain.StateUpdated
import utils.container.WriteableStateContainer
import utils.stupdate.{AbstractStateUpdate, StateUpdate, StateUpdateScript}

/**
 * A service to control some state.
 *
 * @tparam T type of state controlled by this service
 * @param actorEnv actor environment
 * @param stateContainer state container
 * @param serviceName name of the service
 * @param interval interval to check and update state
 * @param tooManyProblemsNumber if number of problems within tooManyProblemsInterval
 *              is greater or equal to this number, then service is disabled (in safe mode)
 * @param tooManyProblemsInterval see tooManyProblemsNumber
 * @param disableOnTooManyProblemsFor if too many problems detected, then the service is disabled
 *                     for this amount of time
 */
abstract class StateControllingService [T] (
                actorEnv : HiPriorityActorEnv,
                stateContainer : WriteableStateContainer [T],
                val serviceName : String,
                interval : TimeValue,
                private[control] val tooManyProblemsNumber : Int = 5,
                private[control] val tooManyProblemsInterval : TimeValue = 10 minutes,
                private[control] val disableOnTooManyProblemsFor : TimeValue = 15 minutes)
          extends AbstractControllingService (actorEnv = actorEnv)
          with StateControllingServiceProblemManager [T]
          with StateControllingServiceDisabler [T]
{
    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Private state

    // reference to scheduled update due to timeout of currently set state
    private var earlyUpdateControl : Option[ScheduleControl] = None

    // a state we set last time
    private var previousState : Option[T] = None

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Customization of behavior

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
     * Called when state is changed (updated). Default implementation
     * logs message defined in transitionMessages map.
     *
     * @param oldState old state (or None if state is changed first time)
     * @param newState new state
     */
    protected def onNewState (oldState : Option[T], newState : T) : Unit = {
        transitionMessages.get (newState) foreach businessLogicInfo
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
     * Find out new state, check for problems and may be update state.
     *
     * @param onlyIfChanged if true, don't update underlaying state controller when new state
     *        is the same as current.
     */
    private[control] def updateState (onlyIfChanged : Boolean = false) : Unit = {
        cancelEarlyUpdate ()

        val problemFound = checkForProblems ()

        // Depending on the detected problems, update state
        if (isDisabled || problemFound) {
            // Something is wrong, fallback to safe state
            doUpdateUsing (new StateUpdate (state = safeState, validTime = interval * 3),
                           onlyIfChanged)
        } else {
            // Everything is good, continuing with update
            getStateUpdate () match {
                case scriptStateUpdate : StateUpdateScript[_] =>
                    // Request to run script
                    doUpdateUsing (scriptStateUpdate, onlyIfChanged)

                case stateUpdate : StateUpdate[_] =>
                    // Request to set some specific state
                    doUpdateUsing (stateUpdate, onlyIfChanged)
            }
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
                val stateUpdated = new StateUpdated (name = serviceName, value = newState)
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
