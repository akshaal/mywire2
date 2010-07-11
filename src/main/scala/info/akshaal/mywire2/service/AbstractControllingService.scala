/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}

import utils.{StateTracker, TemperatureTracker, AbstractTracker, Problem}
import domain.{StateUpdated, StateSensed, Temperature}

/**
 * An abstract implementation of a service that is supposed to control something.
 */
abstract class AbstractControllingService (actorEnv : HiPriorityActorEnv)
                                            extends Actor (actorEnv = actorEnv)
{
    protected final lazy val booleanSensedState =
            new StateTracker[Boolean] (trackedBooleanSensedStateNames : _*)

    protected final lazy val booleanUpdatedState =
            new StateTracker[Boolean] (trackedBooleanUpdatedStateNames : _*)

    protected final lazy val temperature =
            new TemperatureTracker (trackedTemperatureNames : _*)

    protected def trackedBooleanSensedStateNames : List[String] = Nil
    protected def trackedBooleanUpdatedStateNames : List[String] = Nil
    protected def trackedTemperatureNames : List[String] = Nil

    protected def problemIfUndefinedFor = 5 minutes

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

    protected def onTrackedMessageHandled () : Unit = {
    }

    override def start () : Boolean = {
        val started = super.start ()

        if (started) {
            commonPossibleProblems = Nil
            commonPossibleSilentProblems = Nil

            def prepare [T] (list : List[String], tracker : => AbstractTracker[_, _]) (code : => Unit) (implicit m : ClassManifest[T]) {
                if (list != Nil) {
                    subscribe [T] (m)

                    commonPossibleSilentProblems =
                        tracker.problemIfUndefined :: commonPossibleSilentProblems

                    commonPossibleProblems =
                        tracker.problemIfUndefinedFor (problemIfUndefinedFor) :: commonPossibleProblems
                }
            }

            prepare [StateSensed] (trackedBooleanSensedStateNames, booleanSensedState) ()
            prepare [StateUpdated] (trackedBooleanUpdatedStateNames, booleanUpdatedState) ()

            prepare [Temperature] (trackedTemperatureNames, temperature) {
                commonPossibleProblems =
                    temperature.problemIfNaN :: commonPossibleProblems
            }
        }

        started
    }

    /**
     * List of possible problems.
     */
    protected var commonPossibleProblems : List [Problem] = Nil

    /**
     * List of possible problem that don't generate any error messages, but just
     * switches device to the safe mode.
     */
    protected var commonPossibleSilentProblems : List[Problem] = Nil
}
