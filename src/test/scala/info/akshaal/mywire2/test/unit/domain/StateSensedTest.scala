/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.domain

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import domain.StateSensed

class StateSensedTest extends JacoreSpecWithJUnit ("StateSensed object specification") {
    "StateSensed" should {
        "be exported correctly" in {
            val t = new StateSensed ("a", Some(1.0))
            val tm = t.toMap
            tm ("name")  must_==  "a"
            tm ("value")  must_==  1.0

            val t2 = new StateSensed ("b", None)
            val tm2 = t2.toMap
            tm2 ("name")  must_==  "b"
            tm2 ("value")  must beNull
        }
    }
}
