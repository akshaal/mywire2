/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

import info.akshaal.jacore.`package`._

/**
 * High-Precision 1-Wire Digital Thermometer.
 *
 * @param id unique 1-wire identifier
 * @param deviceEnv device environment
 */
class DS18S20 (id : String) (implicit parentDevLoc : DeviceLocation, deviceEnv : OwfsDeviceEnv)
                                extends OwfsDeviceActor (id, "10", parentDevLoc, deviceEnv)
                                   with WithTemperature
