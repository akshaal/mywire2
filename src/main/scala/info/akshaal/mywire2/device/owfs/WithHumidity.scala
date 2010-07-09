/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Operation

import utils.container.HumidityContainer

private[owfs] trait WithHumidity extends HumidityContainer {
    this : OwfsDeviceActor =>

    /**
     * A name of file with humidity.
     */
    protected val humidityFileName : String = "humidity"

    /**
     * Async operation to read humidity from device.
     */
    override def opReadHumidity () : Operation.WithResult [Double] =
                opReadAndParse (humidityFileName, _.toDouble)
}
