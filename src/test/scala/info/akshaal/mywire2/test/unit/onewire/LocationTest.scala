/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.onewire

import org.specs.SpecificationWithJUnit

import onewire.device._

class LocationTest extends SpecificationWithJUnit ("1-wire device location spec") {
    "1-wire device location definition" should {
        "work" in {
            val mp = LocationTestMountPoint
            def path (dev : HasDeviceLocation) = dev.deviceLocation.absolutePath
            
            path (mp.aCoupler.mainBranch)  must_==  "/tmp/test/uncached/1234444/main"
            path (mp.aCoupler.auxBranch)   must_==  "/tmp/test/uncached/1234444/aux"
        }
    }
}

object LocationTestMountPoint extends MountPoint ("/tmp/test") {
    object aCoupler extends DS2409 ("1234444") {
        object mainBranch extends MainBranch {
        }

        object auxBranch extends AuxBranch {
        }
    }
}