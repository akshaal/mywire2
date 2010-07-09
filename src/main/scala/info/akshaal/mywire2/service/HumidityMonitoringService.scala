/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}
import info.akshaal.jacore.utils.OptionDoubleValueFrame

import domain.Humidity
import utils.container.HumidityContainer

/**
 * Read humidity value from device and broadcasts it as exportable object.
 */
class HumidityMonitoringService (actorEnv : HiPriorityActorEnv,
                                 humidityContainer : HumidityContainer,
                                 name : String,
                                 interval : TimeValue)
                     extends Actor (actorEnv = actorEnv)
{
    private val frame = new OptionDoubleValueFrame (3)

    // Load function to broadcast humidity
    private def broadcastHumidity (humidityValue : Option[Double]) : Unit = {
        frame.put (humidityValue)

        val humidity = new Humidity (name = name, value = humidityValue, average3 = frame.average)
        broadcaster.broadcast (humidity)
    }

    schedule every interval executionOf {
        humidityContainer.opReadHumidity () runMatchingResultAsy {
            case Success (humidityValue) =>
                broadcastHumidity (Some (humidityValue))

            case Failure (exc) =>
                error ("Error reading humidity of " + humidityContainer + ": " + exc.getMessage, exc)
                broadcastHumidity (None)
        }
    }

    override def toString() : String = {
        getClass.getSimpleName + "(name=" + name +
            ", humidityContainer=" + humidityContainer + ", interval=" + interval + ")"
    }
}
