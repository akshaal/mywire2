/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package onewire
package device

import com.google.inject.{Inject, Singleton}

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.actor.{Operation, Actor, HiPriorityActorEnv}
import info.akshaal.jacore.fs.text.TextFile

// /////////////////////////////////////////////////////////////////
// Common

/**
 * Device environment.
 */
@Singleton
class DeviceEnv @Inject() (val actorEnv : HiPriorityActorEnv,
                           val textFile : TextFile)
                   extends NotNull

/**
 * An abstract device actor.
 *
 * @param id identifier of the device
 * @param parentDevLoc device location of the parent device (or mount point)
 * @param deviceEnv device environment
 */
abstract class DeviceActor (id : String,
                            parentDevLoc : DeviceLocation,
                            protected val deviceEnv : DeviceEnv)
                        extends Actor (actorEnv = deviceEnv.actorEnv)
                           with Device
{
    override val deviceLocation = new DeviceLocation (parentDevLoc, id)

    /**
     * Makes absoulte path for the device file.
     */
    protected def deviceFile (relativePath : String) : String = {
        deviceLocation.absolutePath + "/" + relativePath
    }

    /**
     * Async operation to read file and parse its content.
     *
     * @param description description
     * @param relativePath a file of the device to read
     * @param parser what parser to use to parse the file
     */
    protected def readAndParse [A] (description : String,
                                    relativePath : String,
                                    parser : String => A)
                                                : Operation.WithResult [A] =
    {
        operation [A] (description) (resultHandler =>
            deviceEnv.textFile.readFile (deviceFile (relativePath)) matchResult {
                case Success (content) =>
                    val result : Result[A] =
                        try {
                            Success (parser (content))
                        } catch {
                            case exc : NumberFormatException =>
                                Failure (exc)
                        }

                    resultHandler (result)

                case Failure (exc) => resultHandler (Failure (exc))
            })
    }
}

/**
 * Trait for device to read temperature.
 */
trait DeviceHasTemperature {
    this : DeviceActor =>

    /**
     * A name of file with temperature.
     */
    protected val temperatureFileName = "temperature"

    /**
     * Async operation to read temperature of device.
     */
    def readTemperature () : Operation.WithResult [Double] =
                readAndParse ("readTemperature", temperatureFileName, _.toDouble)
}

// /////////////////////////////////////////////////////////////////
// DS18S20

/**
 * High-Precision 1-Wire Digital Thermometer.
 * 
 * @param id unique 1-wire identifier
 * @param deviceEnv device environment
 */
class DS18S20 (id : String, deviceEnv : DeviceEnv) (implicit parentDevLoc : DeviceLocation)
                                extends DeviceActor (id, parentDevLoc, deviceEnv)
                                   with DeviceHasTemperature
