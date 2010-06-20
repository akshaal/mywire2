/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.utils

import org.specs.SpecificationWithJUnit

import info.akshaal.jacore.`package`._
import utils.TemperatureTracker
import domain.Temperature

class TemperatureTrackerTest extends SpecificationWithJUnit ("TemperatureTracker specification") {
    "TemperatureTracker" should {
        "must not be created without temperature names" in {
            new TemperatureTracker () must throwA [IllegalArgumentException]
        }

        "should throw exception if untracked temperature requested" in {
            val tt = new TemperatureTracker ("name1")

            tt ("name2")  must throwA [IllegalArgumentException]
        }

        "should return NaN for undefined temperature" in {
            val tt = new TemperatureTracker ("name1")

            tt ("name1").isNaN  must beTrue
        }

        "should return inserted valuse from average3 attribute" in {
            val tt = new TemperatureTracker ("name1")

            tt.updateFrom (new Temperature (name = "name1", value = 1, average3 = 2))
            tt ("name1")  must_==  2

            tt.updateFrom (new Temperature (name = "name1", value = 1, average3 = 10))
            tt ("name1")  must_==  10

            tt.updateFrom (new Temperature (name = "name1", value = Double.NaN, average3 = Double.NaN))
            tt ("name1").isNaN  must beTrue

            tt.updateFrom (new Temperature (name = "name1", value = Double.NaN, average3 = 0))
            tt ("name1")  must_==  0
        }

        "should return true when value updated" in {
            val tt = new TemperatureTracker ("name1", "name2")

            tt.updateFrom (new Temperature (name = "name1", value = 10, average3 = 2))  must_== true
            tt ("name1")  must_==  2
            tt ("name2").isNaN  must beTrue

            tt.updateFrom (new Temperature (name = "name1", value = 10, average3 = -30))  must_== true
            tt ("name1")  must_==  -30
            tt ("name2").isNaN  must beTrue

            tt.updateFrom (new Temperature (name = "name1", value = 10, average3 = -30))  must_== false
            tt ("name1")  must_==  -30
            tt ("name2").isNaN  must beTrue

            tt.updateFrom (new Temperature (name = "name1", value = 10, average3 = 30))  must_== true
            tt ("name1")  must_==  30
            tt ("name2").isNaN  must beTrue

            tt.updateFrom (new Temperature (name = "name2", value = 10, average3 = 15))  must_== true
            tt ("name1")  must_==  30
            tt ("name2")  must_==  15

            tt.updateFrom (new Temperature (name = "name3", value = 10, average3 = 6))  must_== false
            tt ("name1")  must_==  30
            tt ("name2")  must_==  15
        }

        "should detect NaN problem" in {
            val tt = new TemperatureTracker ("name1", "name2")
            tt.problemIfNaN.detected  must_==  None
            tt.problemIfNaN.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = 1, average3 = 1))
            tt.problemIfNaN.detected  must_==  None
            tt.problemIfNaN.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name2", value = 2, average3 = 2))
            tt.problemIfNaN.detected  must_==  None
            tt.problemIfNaN.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = 1, average3 = Double.NaN))
            tt.problemIfNaN.detected  must_!=  None
            tt.problemIfNaN.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = 1, average3 = Double.NaN))
            tt.problemIfNaN.detected  must_!=  None
            tt.problemIfNaN.isGone    must_==  None
        }

        "should detect problem when some of values are undefined" in {
            val tt = new TemperatureTracker ("name1", "name2")
            tt.problemIfUndefinedFor(10 milliseconds).detected  must_==  None
            tt.problemIfUndefinedFor(10 milliseconds).isGone    must_!=  None

            Thread.sleep (15.milliseconds.asMilliseconds)

            tt.problemIfUndefinedFor(10 milliseconds).detected  must_!=  None
            tt.problemIfUndefinedFor(10 milliseconds).isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = 1, average3 = 1))
            tt.problemIfUndefinedFor(10 milliseconds).detected  must_!=  None
            tt.problemIfUndefinedFor(10 milliseconds).isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = 2, average3 = 2))
            tt.problemIfUndefinedFor(10 milliseconds).detected  must_==  None
            tt.problemIfUndefinedFor(10 milliseconds).isGone    must_!=  None

            // Everything is good from the beginning

            val tt2 = new TemperatureTracker ("name1", "name2")
            tt2.updateFrom (new Temperature (name = "name1", value = 1, average3 = 1))
            tt2.updateFrom (new Temperature (name = "name2", value = 2, average3 = 2))

            tt.problemIfUndefinedFor(10 milliseconds).detected  must_==  None
            tt.problemIfUndefinedFor(10 milliseconds).isGone    must_!=  None

            Thread.sleep (15.milliseconds.asMilliseconds)

            tt.problemIfUndefinedFor(10 milliseconds).detected  must_==  None
            tt.problemIfUndefinedFor(10 milliseconds).isGone    must_!=  None
        }

        "should detect problem when a temperature is greater than limit" in {
            val tt = new TemperatureTracker ("name1", "name2")
            val problem = tt.problemIf ("name1").greaterThan (30, backOn = 15)
            
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = 1, average3 = 1))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = 30, average3 = 30))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = 30, average3 = 31))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = 30, average3 = 40))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = 30, average3 = 15))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = 30, average3 = -30))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = 30, average3 = Double.NaN))
            problem.detected  must_==  None
            problem.isGone    must_==  None
        }

        "should detect problem when a temperature is less than limit" in {
            val tt = new TemperatureTracker ("name1", "name2")
            val problem = tt.problemIf ("name1").lessThan (15, backOn = 30)

            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = 1, average3 = 1))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = 15, average3 = 15))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = 14, average3 = 14))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = -5, average3 = -5))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = 30, average3 = 30))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = 40, average3 = 40))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = 40, average3 = Double.NaN))
            problem.detected  must_==  None
            problem.isGone    must_==  None
        }
    }
}
