/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.domain

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import domain.StateUpdated

class StateUpdatedTest extends JacoreSpecWithJUnit ("StateUpdated object specification") {
    "StateUpdated" should {
        "be exported correctly" in {
            val t = new StateUpdated ("a", Some(1.0))
            val tm = t.toMap
            tm ("name")  must_==  "a"
            tm ("value")  must_==  Some(1.0)

            val t2 = new StateUpdated ("b", None)
            val tm2 = t2.toMap
            tm2 ("name")  must_==  "b"
            tm2 ("value")  must_== None

            val t3 = new StateUpdated ("c", null)
            val tm3 = t3.toMap
            tm3 ("name")  must_==  "c"
            tm3 ("value")  must beNull

            val t4 = new StateUpdated ("d", true)
            val tm4 = t4.toMap
            tm4 ("name")  must_==  "d"
            tm4 ("value")  must_==  true
        }
    }
}
