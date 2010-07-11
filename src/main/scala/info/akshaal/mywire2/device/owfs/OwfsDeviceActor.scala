/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Operation, Actor}

import info.akshaal.mywire2.daemon.Autoregister

/**
 * An abstract device actor.
 *
 * @param id identifier of the device
 * @param familyCode family code of the device
 * @param parentDevLoc device location of the parent device (or mount point)
 * @param deviceEnv device environment
 */
abstract class OwfsDeviceActor (id : String,
                                val familyCode : String,
                                parentDevLoc : DeviceLocation,
                                protected val deviceEnv : OwfsDeviceEnv)
                        extends Actor (actorEnv = deviceEnv.actorEnv)
                           with OwfsDevice
                           with HasOnewireFamilyCode
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
                                                Failure ("can't parse" +:+ content)
                                        }

                                    yieldResult (result)

                                case Failure (msg, exc) =>
                                    yieldResult (Failure (msg, exc))
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
