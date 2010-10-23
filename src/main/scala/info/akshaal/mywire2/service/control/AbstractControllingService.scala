/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service.control

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}

import utils.ProblemDetector
import utils.tracker.{StateTracker, TemperatureTracker, AbstractTracker}
import domain.{StateUpdated, StateSensed, Temperature}

/**
 * An abstract implementation of a service that is supposed to control something.
 *
 * @param actorEnv environment for this actor
 */
abstract class AbstractControllingService (actorEnv : HiPriorityActorEnv)
                                            extends Actor (actorEnv = actorEnv)
{
    /**
     * State for boolean sensed states.
     */
    protected final lazy val booleanSensedState =
            new StateTracker[Boolean] (trackedBooleanSensedStateNames : _*)

    /**
     * State for boolean updated states.
     */
    protected final lazy val booleanUpdatedState =
            new StateTracker[Boolean] (trackedBooleanUpdatedStateNames : _*)

    /**
     * State for temperatures.
     */
    protected final lazy val temperature =
            new TemperatureTracker (trackedTemperatureNames : _*)

    /**
     * List of boolean sensed state names to track. Controlling service must
     * override this list if some states needs to be tracked.
     */
    protected def trackedBooleanSensedStateNames : List[String] = Nil

    /**
     * List of boolean updated state names to track. Controlling service must
     * override this list if some states needs to be tracked.
     */
    protected def trackedBooleanUpdatedStateNames : List[String] = Nil

    /**
     * List of temperature names to track. Controlling service must
     * override this list if some temperatures needs to be tracked.
     */
    protected def trackedTemperatureNames : List[String] = Nil

    /**
     * Amount of time that is allowed to pass after a tracker is created
     * and before values for tracked state/temperature/... is gathered.
     */
    protected def problemIfUndefinedFor = 5 minutes

    /**
     * Called when messages for the actor arrives.
     * This implementation handles tracked messages.
     */
    protected override def act() = {
        case stateMsg : StateUpdated =>
            if (booleanUpdatedState.updateFrom (stateMsg)) {
                onTrackedMessageHandled
            }

        case stateMsg : StateSensed =>
            if (booleanSensedState.updateFrom (stateMsg)) {
                onTrackedMessageHandled
            }

        case tempMsg : Temperature =>
            if (temperature.updateFrom (tempMsg)) {
                onTrackedMessageHandled
            }
    }

    /**
     * This method is invoked when a tracked message is handled.
     */
    protected def onTrackedMessageHandled () : Unit = {
    }

    /**
     * List of basic problem detectors.
     */
    protected var basicProblemDetectors : List [ProblemDetector] = Nil

    /**
     * List of basic problem detectors that don't generate any error messages, but just
     * switches device to the safe mode.
     */
    protected var basicSilentProblemDetectors : List[ProblemDetector] = Nil

    // ================================================================================
    // Internal

    /**
     * Subscribes to the tracked messages. Called right before start of actor.
     */
    protected override def beforeStart () : Unit = {
        super.beforeStart ()

        /**
         * Starts tracking specific kind of events, using tracker. Populate common problems
         * that a tracker provides.
         *
         * @tparam T type of messages to subscribe on
         * @param list list of names for the messages of the given type
         * @param tracker tracker that is used to track this kind of messages
         */
        def prepare [T] (list : List [String], tracker : => AbstractTracker [_, _])
                        (code : => Unit)
                        (implicit m : ClassManifest [T])
        {
            if (list != Nil) {
                subscribe [T] (m)

                // If a tracked value is undefined, then there is nothing good, but
                // nothing terrible, just going to safe mode
                basicSilentProblemDetectors ::= tracker.problemIfUndefined

                // But if a value is unavailable for too long, then this is a problem!
                basicProblemDetectors ::=
                    tracker.problemIfUndefinedFor (problemIfUndefinedFor)

                code
            }
        }

        prepare [StateSensed] (trackedBooleanSensedStateNames, booleanSensedState) ()
        prepare [StateUpdated] (trackedBooleanUpdatedStateNames, booleanUpdatedState) ()

        prepare [Temperature] (trackedTemperatureNames, temperature) {
            basicProblemDetectors ::= temperature.problemIfNaN
        }
    }
}
