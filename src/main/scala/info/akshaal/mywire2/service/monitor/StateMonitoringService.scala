/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service.monitor

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}

import domain.StateSensed
import utils.container.ReadableStateContainer

/**
 * Read state value from state container and broadcasts it as exportable object.
 */
class StateMonitoringService[T] (actorEnv : HiPriorityActorEnv,
                                 stateContainer : ReadableStateContainer[T],
                                 serviceName : String,
                                 interval : TimeValue)
                     extends Actor (actorEnv = actorEnv)
{
    // Load function to broadcast humidity
    private def broadcastSensed (state : Option[T]) : Unit = {
        val msg = new StateSensed (name = serviceName, value = state)
        broadcaster.broadcast (msg)
    }

    schedule every interval executionOf {
        stateContainer.opGetState () runMatchingResultAsy {
            case Success (stateValue) =>
                broadcastSensed (Some(stateValue))

            case Failure (msg, excOpt) =>
                error (serviceName +:+ "Error reading state of " + stateContainer
                       +:+ msg +:+ excOpt,
                       excOpt.orNull)
                broadcastSensed (None)
        }
    }

    override def toString() : String = {
        getClass.getSimpleName + "(name=" + serviceName +
            ", humidityDevice=" + stateContainer + ", interval=" + interval + ")"
    }
}

