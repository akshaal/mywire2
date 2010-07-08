/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.onewire

import org.specs.mock.Mockito

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{ActorEnv, Broadcaster}
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import onewire.device._

class LocationTest extends JacoreSpecWithJUnit ("1-wire device location spec") with Mockito {
    implicit val deviceEnv = unit.UnitTestHelper.Mocker.newDeviceEnv

    object mountPoint extends MountPoint ("/tmp/test") {
        object aCoupler extends DS2409 ("1234444") {
            object mainBranch extends MainBranch {
                object temp1 extends DS18S20 ("4a5")
            }

            object auxBranch extends AuxBranch {
            }
        }
    }

    val mp = mountPoint
    def path (dev : HasDeviceLocation) = dev.deviceLocation.absolutePath

    "1-wire device location definition" should {
        "work" in {            
            path (mp.aCoupler.mainBranch)         must_==  "/tmp/test/uncached/1F.1234444/main"
            path (mp.aCoupler.auxBranch)          must_==  "/tmp/test/uncached/1F.1234444/aux"
            path (mp.aCoupler.mainBranch.temp1)   must_==  "/tmp/test/uncached/1F.1234444/main/10.4a5"
        }
    }
}