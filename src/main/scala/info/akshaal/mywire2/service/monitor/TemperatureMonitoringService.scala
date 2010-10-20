/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service.monitor

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}
import info.akshaal.jacore.utils.frame.OptionDoubleValueFrame

import domain.Temperature
import utils.container.TemperatureContainer

/**
 * Reads temperature value from device and broadcasts it as exportable object.
 */
class TemperatureMonitoringService (actorEnv : HiPriorityActorEnv,
                                    temperatureContainer : TemperatureContainer,
                                    serviceName : String,
                                    interval : TimeValue,
                                    illegalTemperature : Option[Double] = None,
                                    maxTries : Int = 3)
                     extends Actor (actorEnv = actorEnv)
{
    // Frame to keep average of temperature values
    private val frame = new OptionDoubleValueFrame (3)

    // How many tries we can perform in case of error, None means that no errors happened yet.
    private var triesLeft : Option[Int] = None

    // Local function to bradcast temperature
    private def broadcastTemperature (temperatureValue : Option[Double]) : Unit = {
        frame.put (temperatureValue)

        val temperature = new Temperature (name = serviceName,
                                           value = temperatureValue,
                                           average3 = frame.average)
        broadcaster.broadcast (temperature)
    }

    // Schedule updates
    schedule every interval executionOf readTemperature()

    // Read temperature
    protected def readTemperature () : Unit =
        temperatureContainer.opReadTemperature () runMatchingResultAsy {
            case Success (temperatureValue) =>
                if (Some (temperatureValue) == illegalTemperature) {
                    warn (serviceName +:+ "Illegal temperature got from "
                          + temperatureContainer + "" + illegalTemperature.get)

                    triesLeft match {
                        case None =>
                            triesLeft = Some (maxTries - 1) // First try
                            readTemperature ()

                        case Some (tries) if tries > 1 =>
                            triesLeft = Some (tries - 1) // We have some tries left
                            readTemperature ()

                        case _ =>
                            triesLeft = None // Can't try anymore
                            broadcastTemperature (None)
                    }
                } else {
                    triesLeft = None // Everything is fine, no need to try
                    broadcastTemperature (Some (temperatureValue))
                }

            case Failure (msg, excOpt) =>
                triesLeft = None // Something is bad, we can't try
                val errorMsg = "Error reading temperature of " + temperatureContainer +:+ msg +:+ excOpt
                error (errorMsg, excOpt.orNull)
                broadcastTemperature (None)
        }

    override def toString() : String = {
        getClass.getSimpleName + "(name=" + serviceName +
            ", temperatureContainer=" + temperatureContainer + ", interval=" + interval + ")"
    }
}