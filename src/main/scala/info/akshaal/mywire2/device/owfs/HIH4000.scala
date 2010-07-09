/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

/**
 * Trait for device that use HIH4000 to read humidity.
 */
trait HIH4000 {
    this : WithHumidity =>

    /**
     * A name of file with humidity.
     */
    protected override val humidityFileName = "HIH4000/humidity"
}
