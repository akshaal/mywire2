/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package onewire
package service

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}
import info.akshaal.jacore.scheduler.ScheduleControl

import device.{DeviceHasTemperature, DeviceHasHumidity}
import domain.{Temperature, Humidity, StateUpdated}
import utils.{StateContainer, StateUpdate}

/**
 * Reads temperature value from device and broadcasts it as exportable object.
 */
class TemperatureMonitoringService (actorEnv : HiPriorityActorEnv,
                                    temperatureDevice : DeviceHasTemperature,
                                    name : String,
                                    interval : TimeValue,
                                    illegalTemperature : Option[Double] = None)
                     extends Actor (actorEnv = actorEnv)
{
    // Local function to bradcast temperature
    private def broadcastTemperature (temperatureValue : Double) : Unit = {
        val temperature = new Temperature (name = name, value = temperatureValue)
        broadcaster.broadcast (temperature)
    }

    schedule every interval executionOf {
        temperatureDevice.opReadTemperature () runMatchingResultAsy {
            case Success (temperatureValue) =>
                if (Some (temperatureValue) == illegalTemperature) {
                    warn ("Illegal temperature got from " + temperatureDevice + "" + illegalTemperature.get)
                    broadcastTemperature (Double.NaN)
                } else {
                    broadcastTemperature (temperatureValue)
                }

            case Failure (exc) =>
                error ("Error reading temperature of " + temperatureDevice + ": "
                       + exc.getMessage, exc)
                broadcastTemperature (Double.NaN)
        }
    }

    override def toString : String = {
        getClass.getSimpleName + "(name=" + name +
            ", temperatureDevice=" + temperatureDevice + ", interval=" + interval + ")"
    }
}

/**
 * Read humidity value from device and broadcasts it as exportable object.
 */
class HumidityMonitoringService (actorEnv : HiPriorityActorEnv,
                                 humidityDevice : DeviceHasHumidity,
                                 name : String,
                                 interval : TimeValue)
                     extends Actor (actorEnv = actorEnv)
{
    // Load function to broadcast humidity
    private def broadcastHumidity (humidityValue : Double) : Unit = {
        val humidity = new Humidity (name = name, value = humidityValue)
        broadcaster.broadcast (humidity)
    }

    schedule every interval executionOf {
        humidityDevice.opReadHumidity () runMatchingResultAsy {
            case Success (humidityValue) =>
                broadcastHumidity (humidityValue)

            case Failure (exc) =>
                error ("Error reading humidity of " + humidityDevice + ": " + exc.getMessage, exc)
                broadcastHumidity (Double.NaN)
        }
    }

    override def toString : String = {
        getClass.getSimpleName + "(name=" + name +
            ", humidityDevice=" + humidityDevice + ", interval=" + interval + ")"
    }
}

/**
 * A service to control some state.
 * @param actorEnv actor environment
 * @param stateContainer state container
 * @param name name
 * @param interval interval to check and update state
 */
abstract class StateControllingService [T] (actorEnv : HiPriorityActorEnv,
                                            stateContainer : StateContainer [T],
                                            name : String,
                                            interval : TimeValue)
                            extends Actor (actorEnv = actorEnv)
{
    private var earlyUpdateControl : Option[ScheduleControl] = None
    private var previousState : Option[T] = None

    schedule every interval executionOf updateState

    /**
     * Update state.
     */
    protected def updateState () {
        earlyUpdateControl foreach (_.cancel ())
        earlyUpdateControl = None

        val stateUpdate = getStateUpdate ()

        // Change state
        stateContainer.opSetState (stateUpdate.state) runMatchingResultAsy {
            case Success (_) =>
                previousState foreach (state => {
                    if (state != stateUpdate.state) {
                        val stateUpdated = new StateUpdated (name = name, value = stateUpdate.state)
                        broadcaster.broadcast (stateUpdated)
                    }
                })
            
                previousState = Some (stateUpdate.state)

            case Failure (exc) =>
                error ("Error setting state of " + stateContainer + ": " + exc.getMessage, exc)
        }

        // Schedule next state update
        if (stateUpdate.validTime > (0 nanoseconds) && stateUpdate.validTime < (interval * 2)) {
            earlyUpdateControl = Some (schedule in stateUpdate.validTime executionOf updateState)
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
     * Get new state.
     */
    protected def getStateUpdate () : StateUpdate[T]
}