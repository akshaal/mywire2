/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package onewire
package device

import com.google.inject.{Inject, Singleton}

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Operation, Actor, HiPriorityActorEnv}
import info.akshaal.jacore.fs.text.TextFile

import info.akshaal.mywire2.daemon.Autoregister
import utils.StateContainer

// /////////////////////////////////////////////////////////////////
// Common

/**
 * For devices that are support family code.
 */
trait HasFamilyCode {
    val familyCode : String
}

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
 * @param familyCode family code of the device
 * @param parentDevLoc device location of the parent device (or mount point)
 * @param deviceEnv device environment
 */
abstract class DeviceActor (id : String,
                            val familyCode : String,
                            parentDevLoc : DeviceLocation,
                            protected val deviceEnv : DeviceEnv)
                        extends Actor (actorEnv = deviceEnv.actorEnv)
                           with Device
                           with HasFamilyCode
                           with Autoregister
{
    override val deviceLocation = new DeviceLocation (parentDevLoc, familyCode + "." + id)
    override val registrationName = id

    /**
     * Masimum file size that this device might have.
     */
    protected val maxFileSize = 1024

    /**
     * Makes absoulte path for the device file.
     */
    protected def deviceFile (relativePath : String) : String = {
        deviceLocation.absolutePath + "/" + relativePath
    }

    /**
     * Async operation to read file and parse its content.
     *
     * @param relativePath a file of the device to read
     * @param parser what parser to use to parse the file
     */
    protected def opReadAndParse [A] (relativePath : String,
                                      parser : String => A)
                                 : Operation.WithResult [A] =
    {
        new AbstractOperation [Result[A]] {
            override def processRequest () {
                deviceEnv.textFile
                         .opReadFile (deviceFile (relativePath), Some(maxFileSize))
                         .runMatchingResultAsy {
                                case Success (content) =>
                                    val result : Result[A] =
                                        try {
                                            Success (parser (content))
                                        } catch {
                                            case exc : NumberFormatException =>
                                                Failure (exc)
                                        }

                                    yieldResult (result)

                                case Failure (exc) =>
                                    yieldResult (Failure (exc))
                            }
            }
        }
    }

    /**
     * Async operation to write to file
     *
     * @param relativePath a file of the device to read
     * @param content new content for the file
     */
    protected def opWrite (relativePath : String, content : String) : Operation.WithResult [Unit] =
            deviceEnv.textFile.opWriteFile (deviceFile (relativePath), content)

    /**
     * Give a description to the object.
     */
    override def toString : String = {
        if (deviceLocation == null) {
            // Object is not finished initialization
            super.toString
        } else {
            val clazzName =
                if (getClass.getSimpleName.endsWith("$")) {
                    getClass.getSuperclass.getSimpleName
                } else {
                    getClass.getSimpleName
                }

            clazzName + "(" + deviceLocation.absolutePath + ")"
        }
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
     * Async operation to read temperature from device.
     */
    def opReadTemperature () : Operation.WithResult [Double] =
                opReadAndParse (temperatureFileName, _.toDouble)
}

/**
 * Trait for device to read humidity.
 */
trait DeviceHasHumidity {
    this : DeviceActor =>

    /**
     * A name of file with humidity.
     */
    protected val humidityFileName : String = "humidity"

    /**
     * Async operation to read humidity from device.
     */
    def opReadHumidity () : Operation.WithResult [Double] =
                opReadAndParse (humidityFileName, _.toDouble)
}

/**
 * Trait for device that use HIH4000 to read humidity.
 */
trait HIH4000 {
    this : DeviceHasHumidity =>

    /**
     * A name of file with humidity.
     */
    protected override val humidityFileName = "HIH4000/humidity"
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
                                extends DeviceActor (id, "10", parentDevLoc, deviceEnv)
                                   with DeviceHasTemperature

/**
 * Smart Battery Monitor.
 *
 * @param id unique 1-wire identifier
 * @param deviceEnv device environment
 */
class DS2438 (id : String, deviceEnv : DeviceEnv) (implicit parentDevLoc : DeviceLocation)
                                extends DeviceActor (id, "26", parentDevLoc, deviceEnv)
                                   with DeviceHasTemperature
                                   with DeviceHasHumidity

/**
 * Addressable swtich.
 * @param id unique 1-wire identifier
 * @param deviceEnv device environment
 */
class DS2405 (id : String, deviceEnv : DeviceEnv) (implicit parentDevLoc : DeviceLocation)
                                extends DeviceActor (id, "05", parentDevLoc, deviceEnv)
                                   with StateContainer[Boolean]
{
    /**
     * A name of file with PIO state.
     */
    protected final val pioFileName : String = "PIO"

    /**
     * Parse state.
     */
    protected def parseState (state : String) : Boolean =
        state match {
            case "0" => false

            case "1" => true

            case x => throw new NumberFormatException ("Unknown state: " + x)
        }

    /**
     * Async operation to read current PIO state. This is state that was set previously.
     * This is not sensed state (actual state).
     */
    def opGetState () : Operation.WithResult [Boolean] =
                opReadAndParse (pioFileName, parseState)

    /**
     * Write new state for the PIO.
     */
    def opSetState (state : Boolean) : Operation.WithResult [Unit] =
                opWrite (pioFileName, if (state) "1" else "0")
}