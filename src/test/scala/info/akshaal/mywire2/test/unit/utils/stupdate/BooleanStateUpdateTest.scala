/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.utils.stupdate

import info.akshaal.jacore.test.JacoreSpecWithJUnit

import info.akshaal.jacore.`package`._
import utils.stupdate.BooleanStateUpdate

import BooleanStateUpdate._

class BooleanStateUpdateTest extends JacoreSpecWithJUnit ("BooleanStateUpdate specification") {
    val true1 = new BooleanStateUpdate (state = true, validTime = 1 milliseconds)
    val true2 = new BooleanStateUpdate (state = true, validTime = 2 milliseconds)
    val true3 = new BooleanStateUpdate (state = true, validTime = 3 milliseconds)
    val false1 = new BooleanStateUpdate (state = false, validTime = 1 milliseconds)
    val false2 = new BooleanStateUpdate (state = false, validTime = 2 milliseconds)
    val false3 = new BooleanStateUpdate (state = false, validTime = 3 milliseconds)

    "BooleanStateUpdate" should {
        "have if-then-else method" in {
            true1.ifThenElse (true2, true3)  must_==  true2
            true1.ifThenElse (true3, false2)  must_==  true3
            true2.ifThenElse (false1, true2)  must_==  false1
            false1.ifThenElse (true2, false3)  must_==  false3
            false1.ifThenElse (true3, true2)  must_==  true2
            false2.ifThenElse (true1, true2)  must_==  true2
        }

        "have and method" in {
            true1 and true2  must_==  true1
            true2 and true1  must_==  true1
            true3 and true1  must_==  true1
            false1 and false2  must_==  false2
            false2 and false1  must_==  false2
            false3 and false1  must_==  false3
            true1 and false2  must_==  false2
            true2 and false1  must_==  false1
            true3 and false1  must_==  false1
            false1 and true2  must_==  false1
            false2 and true1  must_==  false2
            false3 and true1  must_==  false3
        }

        "have && method" in {
            true1 && true2  must_==  true1
            true2 && true1  must_==  true1
            true3 && true1  must_==  true1
            false1 && false2  must_==  false2
            false2 && false1  must_==  false2
            false3 && false1  must_==  false3
            true1 && false2  must_==  false2
            true2 && false1  must_==  false1
            true3 && false1  must_==  false1
            false1 && true2  must_==  false1
            false2 && true1  must_==  false2
            false3 && true1  must_==  false3
        }

        "have or method" in {
            true1 or true2  must_==  true2
            true2 or true1  must_==  true2
            true3 or true1  must_==  true3
            false1 or false2  must_==  false1
            false2 or false1  must_==  false1
            false3 or false2  must_==  false2
            true1 or false2  must_==  true1
            true2 or false1  must_==  true2
            true3 or false1  must_==  true3
            false1 or true2  must_==  true2
            false2 or true1  must_==  true1
            false3 or true1  must_==  true1
        }

        "have or method" in {
            true1 || true2  must_==  true2
            true2 || true1  must_==  true2
            true3 || true1  must_==  true3
            false1 || false2  must_==  false1
            false2 || false1  must_==  false1
            false3 || false2  must_==  false2
            true1 || false2  must_==  true1
            true2 || false1  must_==  true2
            true3 || false1  must_==  true3
            false1 || true2  must_==  true2
            false2 || true1  must_==  true1
            false3 || true1  must_==  true1
        }

        "have ! method" in {
            !true1  must_==  false1
            !false1  must_==  true1

            !true2  must_==  false2
            !false2  must_==  true2
            
            !true3  must_==  false3
            !false3  must_==  true3
        }

        "have implicit method to convert from boolean to BooleanStateUpdate" in {
            def c (b : BooleanStateUpdate) : BooleanStateUpdate = b

            c(true)  must_==  new BooleanStateUpdate (state = true, validTime = Long.MaxValue nanoseconds)
            c(false)  must_==  new BooleanStateUpdate (state = false, validTime = Long.MaxValue nanoseconds)
        }
    }
}
