/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.utils

import org.specs.SpecificationWithJUnit

import info.akshaal.jacore.`package`._
import utils.TemperatureTracker

class TemperatureTrackerTest extends SpecificationWithJUnit ("TemperatureTracker specification") {
    "TemperatureTracker" should {
        "must not be created without temperature names" in {
            new TemperatureTracker () must throwA [IllegalArgumentException]
        }
    }
}
