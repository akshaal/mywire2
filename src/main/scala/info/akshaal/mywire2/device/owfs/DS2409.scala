/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

/**
 * MicroLAN Coupler.
 */
case class DS2409 (id : String) (implicit parentDeviceLocation : DeviceLocation)
                            extends OwfsLocationDevice
                               with HasOnewireFamilyCode
{
    private val ds2409 = this
    override val familyCode = "1F"
    override val deviceLocation =
                new DeviceLocation (parentDeviceLocation, familyCode + "." + id)

    protected abstract sealed class Branch (name : String) extends HasDeviceLocation {
        override implicit val deviceLocation = new DeviceLocation (ds2409.deviceLocation, name)
    }
    
    protected case class MainBranch () extends Branch ("main")
    protected case class AuxBranch () extends Branch ("aux")
}
