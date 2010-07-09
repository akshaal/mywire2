/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

/**
 * Mount point. Describes a place where 1-wire devices are projected by OWFS
 */
case class OwfsMountPoint (absolutePath : String) extends OwfsLocationDevice {
    protected implicit val _ = this
    override implicit val deviceLocation = new DeviceLocation (absolutePath + "/uncached")
}
