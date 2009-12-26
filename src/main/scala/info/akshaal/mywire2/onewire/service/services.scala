/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package onewire
package service

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}

import device.DeviceHasTemperature

/**
 * Read temperature value from device and broadcasts it as exportable object.
 */
class TemperatureMonitoringService (actorEnv : HiPriorityActorEnv,
                                    temperatureDevice : DeviceHasTemperature,
                                    name : String,
                                    interval : TimeUnit)
                     extends Actor (actorEnv = actorEnv)
{
    schedule every interval executionOf {
        temperatureDevice.readTemperature () matchResult {
            case Success (temperature) =>
                // TODO

            case Failure (exc) =>
                // TODO
        }
    }

    override def toString : String = {
        getClass.getSimpleName + "(name=" + name + ", interval=" + interval + ")"
    }
}