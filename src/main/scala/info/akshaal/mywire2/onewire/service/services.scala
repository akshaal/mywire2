/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package onewire
package service

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}

import device.DeviceHasTemperature
import domain.Temperature

/**
 * Read temperature value from device and broadcasts it as exportable object.
 */
class TemperatureMonitoringService (actorEnv : HiPriorityActorEnv,
                                    temperatureDevice : DeviceHasTemperature,
                                    name : String,
                                    interval : TimeValue)
                     extends Actor (actorEnv = actorEnv)
{
    schedule every interval executionOf {
        temperatureDevice.opReadTemperature () runMatchingResultAsy {
            case Success (temperatureValue) =>
                val temperature = new Temperature (name = name, value = temperatureValue)
                broadcaster.broadcast (temperature)

            case Failure (exc) =>
                error ("Error reading temperature of " + temperatureDevice + ": "
                       + exc.getMessage, exc)
        }
    }

    override def toString : String = {
        getClass.getSimpleName + "(name=" + name +
            ", temperatureDevice=" + temperatureDevice + ", interval=" + interval + ")"
    }
}
