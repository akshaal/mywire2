/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device

/**
 * Specifies that the given object is either a device or a container for other devices.
 */
trait HasDeviceLocation {
    val deviceLocation : DeviceLocation
}
