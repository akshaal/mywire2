/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.utils

import info.akshaal.jacore.test.JacoreSpecWithJUnit

import info.akshaal.jacore.`package`._
import utils.StateTracker
import domain.StateUpdated

class StateTrackerTest extends JacoreSpecWithJUnit ("StateTracker specification") {
    "StateTracker" should {
        "must not be created without state names" in {
            new StateTracker[Boolean] () must throwA [IllegalArgumentException]
        }

        "should throw exception if untracked state requested" in {
            val st = new StateTracker[Boolean] ("name1")

            st ("name2")  must throwA [IllegalArgumentException]
        }

        "should throw exception for undefined state" in {
            val st = new StateTracker[Boolean] ("name1")

            st ("name1")  must throwA[IllegalArgumentException]
        }

        "should return inserted value" in {
            val st = new StateTracker[Int] ("name1")

            st.updateFrom (new StateUpdated (name = "name1", value =3))
            st ("name1")  must_==  3

            st.updateFrom (new StateUpdated (name = "name1", value = 10))
            st ("name1")  must_==  10

            st.updateFrom (new StateUpdated (name = "name1", value = 55))
            st ("name1")  must_==  55

            st.updateFrom (new StateUpdated (name = "name1", value = 0))
            st ("name1")  must_==  0
        }

        "should return true when value updated" in {
            val st = new StateTracker[Boolean] ("name1", "name2")

            st.updateFrom (new StateUpdated (name = "name1", value = false))  must_== true
            st ("name1")  must_==  false
            st ("name2")  must throwA[IllegalArgumentException]

            st.updateFrom (new StateUpdated (name = "name1", value = true))  must_== true
            st ("name1")  must_==  true
            st ("name2")  must throwA[IllegalArgumentException]

            st.updateFrom (new StateUpdated (name = "name1", value = true))  must_== false
            st ("name1")  must_==  true
            st ("name2")  must throwA[IllegalArgumentException]

            st.updateFrom (new StateUpdated (name = "name1", value = false))  must_== true
            st ("name1")  must_==  false
            st ("name2")  must throwA[IllegalArgumentException]

            st.updateFrom (new StateUpdated (name = "name2", value = false))  must_== true
            st ("name1")  must_==  false
            st ("name2")  must_==  false

            st.updateFrom (new StateUpdated (name = "name3", value = true))  must_== false
            st ("name1")  must_==  false
            st ("name2")  must_==  false
        }

        "should detect problem when some of values are undefined for too long" in {
            val st = new StateTracker[Boolean] ("name1", "name2")
            st.problemIfUndefinedFor(10 milliseconds).detected  must_==  None
            st.problemIfUndefinedFor(10 milliseconds).isGone    must_!=  None

            Thread.sleep (15.milliseconds.asMilliseconds)

            st.problemIfUndefinedFor(10 milliseconds).detected  must_!=  None
            st.problemIfUndefinedFor(10 milliseconds).isGone    must_==  None

            st.updateFrom (new StateUpdated (name = "name1", value = true))
            st.problemIfUndefinedFor(10 milliseconds).detected  must_!=  None
            st.problemIfUndefinedFor(10 milliseconds).isGone    must_==  None

            st.updateFrom (new StateUpdated (name = "name2", value = true))
            st.problemIfUndefinedFor(10 milliseconds).detected  must_==  None
            st.problemIfUndefinedFor(10 milliseconds).isGone    must_!=  None

            // Everything is good from the beginning

            val st2 = new StateTracker[Int] ("name1", "name2")
            st2.updateFrom (new StateUpdated (name = "name1", value = 1))
            st2.updateFrom (new StateUpdated (name = "name2", value = 2))

            st2.problemIfUndefinedFor(10 milliseconds).detected  must_==  None
            st2.problemIfUndefinedFor(10 milliseconds).isGone    must_!=  None

            Thread.sleep (15.milliseconds.asMilliseconds)

            st2.problemIfUndefinedFor(10 milliseconds).detected  must_==  None
            st2.problemIfUndefinedFor(10 milliseconds).isGone    must_!=  None
        }

        "should detect problem when a state is illegal" in {
            val st = new StateTracker[Int] ("name1", "name2")
            val problem = st.problemIf ("name1").equalsTo(5, 10)
            
            problem.detected  must_==  None
            problem.isGone    must_==  None

            st.updateFrom (new StateUpdated (name = "name2", value = 1))
            problem.detected  must_==  None
            problem.isGone    must_==  None

            st.updateFrom (new StateUpdated (name = "name1", value = 1))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            st.updateFrom (new StateUpdated (name = "name1", value = 5))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            st.updateFrom (new StateUpdated (name = "name1", value = 50))
            problem.detected  must_==  None
            problem.isGone    must_!=  None

            st.updateFrom (new StateUpdated (name = "name1", value = 10))
            problem.detected  must_!=  None
            problem.isGone    must_==  None

            st.updateFrom (new StateUpdated (name = "name1", value = 11))
            problem.detected  must_==  None
            problem.isGone    must_!=  None
        }

        "should detect problem when some of values are undefined" in {
            val st = new StateTracker[Boolean] ("name1", "name2")
            st.problemIfUndefined().detected  must_!=  None
            st.problemIfUndefined().isGone    must_==  None

            Thread.sleep (15.milliseconds.asMilliseconds)

            st.problemIfUndefined().detected  must_!=  None
            st.problemIfUndefined().isGone    must_==  None

            st.updateFrom (new StateUpdated (name = "name1", value = true))
            st.problemIfUndefined().detected  must_!=  None
            st.problemIfUndefined().isGone    must_==  None

            st.updateFrom (new StateUpdated (name = "name2", value = true))
            st.problemIfUndefined().detected  must_==  None
            st.problemIfUndefined().isGone    must_!=  None
        }
    }
}
