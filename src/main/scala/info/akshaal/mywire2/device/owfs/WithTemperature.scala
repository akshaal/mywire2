/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Operation

import utils.container.TemperatureContainer

private[owfs] trait WithTemperature extends TemperatureContainer {
    this : OwfsDeviceActor =>

    /**
     * A name of file with temperature.
     */
    protected val temperatureFileName = "temperature"

    /**
     * Async operation to read temperature from device.
     */
    override def opReadTemperature () : Operation.WithResult [Double] =
                opReadAndParse (temperatureFileName, _.toDouble)
}
