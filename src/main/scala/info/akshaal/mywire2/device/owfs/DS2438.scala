/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

/**
 * Smart Battery Monitor.
 *
 * @param id unique 1-wire identifier
 * @param deviceEnv device environment
 */
class DS2438 (id : String) (implicit parentDevLoc : DeviceLocation, deviceEnv : OwfsDeviceEnv)
                                extends OwfsDeviceActor (id, "26", parentDevLoc, deviceEnv)
                                   with WithTemperature
                                   with WithHumidity

