/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.utils.tracker

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit
import utils.tracker.TemperatureTracker
import domain.Temperature

class TemperatureTrackerTest extends JacoreSpecWithJUnit ("TemperatureTracker specification") {
    "TemperatureTracker" should {
        "must not be created without temperature names" in {
            new TemperatureTracker () must throwA [IllegalArgumentException]
        }

        "should throw exception if untracked temperature requested" in {
            val tt = new TemperatureTracker ("name1")

            tt ("name2")  must throwA [IllegalArgumentException]
        }

        "should throw exception for undefined temperature" in {
            val tt = new TemperatureTracker ("name1")

            tt ("name1")  must throwA[IllegalArgumentException]
        }

        "should return inserted valuse from average3 attribute" in {
            val tt = new TemperatureTracker ("name1")

            tt.updateFrom (new Temperature (name = "name1", value = Some(1), average3 = Some(2)))
            tt ("name1")  must_==  2

            tt.updateFrom (new Temperature (name = "name1", value = Some(1), average3 = Some(10)))
            tt ("name1")  must_==  10

            tt.updateFrom (new Temperature (name = "name1", value = None, average3 = None))
            tt ("name1")  must throwA[IllegalArgumentException]

            tt.updateFrom (new Temperature (name = "name1", value = None, average3 = Some(0)))
            tt ("name1")  must_==  0
        }

        "should return true when value updated" in {
            val tt = new TemperatureTracker ("name1", "name2")

            tt.updateFrom (new Temperature (name = "name1", value = Some(10), average3 = Some(2)))  must_== true
            tt ("name1")  must_==  2
            tt ("name2")  must throwA[IllegalArgumentException]

            tt.updateFrom (new Temperature (name = "name1", value = Some(10), average3 = Some(-30)))  must_== true
            tt ("name1")  must_==  -30
            tt ("name2")  must throwA[IllegalArgumentException]

            tt.updateFrom (new Temperature (name = "name1", value = Some(10), average3 = Some(-30)))  must_== false
            tt ("name1")  must_==  -30
            tt ("name2")  must throwA[IllegalArgumentException]

            tt.updateFrom (new Temperature (name = "name1", value = Some(10), average3 = Some(30)))  must_== true
            tt ("name1")  must_==  30
            tt ("name2")  must throwA[IllegalArgumentException]

            tt.updateFrom (new Temperature (name = "name2", value = Some(10), average3 = Some(15)))  must_== true
            tt ("name1")  must_==  30
            tt ("name2")  must_==  15

            tt.updateFrom (new Temperature (name = "name3", value = Some(10), average3 = Some(6)))  must_== false
            tt ("name1")  must_==  30
            tt ("name2")  must_==  15
        }

        "should detect NaN problem" in {
            val tt = new TemperatureTracker ("name1", "name2")
            tt.problemIfNaN.detected  must_==  None
            tt.problemIfNaN.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(1), average3 = Some(1)))
            tt.problemIfNaN.detected  must_==  None
            tt.problemIfNaN.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name2", value = Some(2), average3 = Some(2)))
            tt.problemIfNaN.detected  must_==  None
            tt.problemIfNaN.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(1), average3 = Some(Double.NaN)))
            tt.problemIfNaN.detected  must_!=  None
            tt.problemIfNaN.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = Some(1), average3 = Some(Double.NaN)))
            tt.problemIfNaN.detected  must_!=  None
            tt.problemIfNaN.isGone    must_==  None
        }

        "should detect problem when some of values are undefined for too long" in {
            val tt = new TemperatureTracker ("name1", "name2")
            tt.problemIfUndefinedFor(50 milliseconds).detected  must_==  None
            tt.problemIfUndefinedFor(50 milliseconds).isGone    must_!=  None

            Thread.sleep (55.milliseconds.asMilliseconds)

            tt.problemIfUndefinedFor(50 milliseconds).detected  must_!=  None
            tt.problemIfUndefinedFor(50 milliseconds).isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(1), average3 = Some(1)))
            tt.problemIfUndefinedFor(50 milliseconds).detected  must_!=  None
            tt.problemIfUndefinedFor(50 milliseconds).isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = Some(2), average3 = Some(2)))
            tt.problemIfUndefinedFor(50 milliseconds).detected  must_==  None
            tt.problemIfUndefinedFor(50 milliseconds).isGone    must_!=  None

            // Everything is good from the beginning

            val tt2 = new TemperatureTracker ("name1", "name2")
            tt2.updateFrom (new Temperature (name = "name1", value = Some(1), average3 = Some(1)))
            tt2.updateFrom (new Temperature (name = "name2", value = Some(2), average3 = Some(2)))

            tt2.problemIfUndefinedFor(50 milliseconds).detected  must_==  None
            tt2.problemIfUndefinedFor(50 milliseconds).isGone    must_!=  None

            Thread.sleep (55.milliseconds.asMilliseconds)

            tt2.problemIfUndefinedFor(50 milliseconds).detected  must_==  None
            tt2.problemIfUndefinedFor(50 milliseconds).isGone    must_!=  None
        }

        "should detect problem when a temperature is greater than limit" in {
            val tt = new TemperatureTracker ("name1", "name2")
            val problem = tt.problemIf ("name1").greaterThan (30, backOn = 15)
            
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = Some(1), average3 = Some(1)))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(30), average3 = Some(30)))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(30), average3 = Some(31)))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(30), average3 = Some(40)))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(30), average3 = Some(15)))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(30), average3 = Some(-30)))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(30), average3 = None))
            problem.detected  must_==  None
            problem.isGone    must_==  None
        }

        "should detect problem when a temperature is less than limit" in {
            val tt = new TemperatureTracker ("name1", "name2")
            val problem = tt.problemIf ("name1").lessThan (15, backOn = 30)

            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = Some(1), average3 = Some(1)))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(15), average3 = Some(15)))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(14), average3 = Some(14)))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(-5), average3 = Some(-5)))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(30), average3 = Some(30)))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(40), average3 = Some(40)))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(40), average3 = None))
            problem.detected  must_==  None
            problem.isGone    must_==  None
        }

        "should detect problem when some of values are undefined" in {
            val tt = new TemperatureTracker ("name1", "name2")
            tt.problemIfUndefined().detected  must_!=  None
            tt.problemIfUndefined().isGone    must_==  None

            Thread.sleep (15.milliseconds.asMilliseconds)

            tt.problemIfUndefined().detected  must_!=  None
            tt.problemIfUndefined().isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name1", value = Some(1), average3 = Some(1)))
            tt.problemIfUndefined().detected  must_!=  None
            tt.problemIfUndefined().isGone    must_==  None

            tt.updateFrom (new Temperature (name = "name2", value = Some(2), average3 = Some(2)))
            tt.problemIfUndefined().detected  must_==  None
            tt.problemIfUndefined().isGone    must_!=  None
        }
    }
}
