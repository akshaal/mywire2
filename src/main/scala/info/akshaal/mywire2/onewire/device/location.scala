/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package onewire
package device

/**
 * Class describes device location in the filesystem.
 */
sealed case class DeviceLocation (absolutePath : String) extends NotNull {
    def this (parent : DeviceLocation, relativePath : String) =
            this (parent.absolutePath + "/" + relativePath)
}

/**
 * Specifies that the given object is either a device or a container for other devices.
 */
trait HasDeviceLocation {
    val deviceLocation : DeviceLocation
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// LocationDevice classes

/**
 * An abstract device the sole role of which is to host other devices.
 */
abstract sealed class LocationDevice extends Device

/**
 * Mount point. Describes a place where 1-wire devices are projected by OWFS to
 */
case class MountPoint (absolutePath : String) extends LocationDevice {
    protected implicit val _ = this
    override implicit val deviceLocation = new DeviceLocation (absolutePath + "/uncached")
}

/**
 * MicroLAN Coupler.
 */
case class DS2409 (id : String) (implicit parentDeviceLocation : DeviceLocation)
                            extends LocationDevice
{
    protected implicit val _ = this
    override implicit val deviceLocation = new DeviceLocation (parentDeviceLocation, id)
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Branch classes

abstract sealed class Branch (parentDeviceLocation : DeviceLocation, name : String)
                                                    extends HasDeviceLocation
{
    override implicit val deviceLocation = new DeviceLocation (parentDeviceLocation, name)
}

case class MainBranch (implicit ds2409 : DS2409) extends Branch (ds2409.deviceLocation, "main")
case class AuxBranch (implicit ds2409 : DS2409) extends Branch (ds2409.deviceLocation, "aux")