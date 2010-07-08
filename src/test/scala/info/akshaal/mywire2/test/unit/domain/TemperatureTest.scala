/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.domain

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import domain.Temperature

class TemperatureTest extends JacoreSpecWithJUnit ("Temperature object specification") {
    "Temperature" should {
        "be exported correctly" in {
            val t = new Temperature ("a", Some(1.0), Some(2.0))
            val tm = t.toMap
            tm ("name")  must_==  "a"
            tm ("value")  must_==  1.0
            tm ("average3")  must_==  2.0

            val t2 = new Temperature ("b", None, Some(4.0))
            val tm2 = t2.toMap
            tm2 ("name")  must_==  "b"
            tm2 ("value")  must beNull
            tm2 ("average3")  must_==  4.0

            val t3 = new Temperature ("c", Some(10.0), None)
            val tm3 = t3.toMap
            tm3 ("name")  must_==  "c"
            tm3 ("value")  must_==  10.0
            tm3 ("average3")  must beNull
        }
    }
}
