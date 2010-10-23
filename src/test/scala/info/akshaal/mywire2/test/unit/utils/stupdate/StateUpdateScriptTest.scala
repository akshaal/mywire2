/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.utils.stupdate

import scala.util.continuations._

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import utils.stupdate.StateUpdateScript

class StateUpdateScriptTest extends JacoreSpecWithJUnit ("StateUpdateScript specification") {
    // If this is placed in some other place, like in "... in {}" clause, then scala
    // is not able to compile it. So all scripts are placed here.

    val scriptA =
        new StateUpdateScript[Boolean] {
            var p1 = 0
            var p2 = 0
            var p3 = 0
            var p4 = 0

            protected override def run () : Unit @suspendable = {
                p1 += 1
                set (true)
                p2 += 1
                wait (1 minutes)
                p3 += 1
                set (false)
                p4 += 1
            }
        }

    val scriptB =
        new StateUpdateScript[Boolean] {
            var p1 = 0
            var p2 = 0
            var di = 0

            protected override def run () : Unit @suspendable = {
                p1 += 1
                set (true)
                p2 += 1
            }

            protected override def defaultOnInterrupt () {
                di += 1
            }
        }

    val scriptC =
        new StateUpdateScript[Boolean] {
            var p1 = 0
            var p2 = 0
            var di = 0
            var i1 = 0

            protected override def run () : Unit @suspendable = {
                p1 += 1

                setOrFail (true) {
                    i1 += 1
                }

                p2 += 1
            }

            protected override def defaultOnInterrupt () {
                di += 1
            }
        }

    // - - - - - - - - - - - - - - - - - - - - - - - -- - - - - - - - -  -- - - - - - - - -
    // The following two scripts are used to test equality. These scripts must be considered
    // inequal! Note that, those are defs and they return new object on each invokation!

    def scriptD () =
        new StateUpdateScript[Boolean] {
            def run () = {}
        }

    def scriptD2 (x : Int) =
        new StateUpdateScript[Boolean] {
            val y = x

            def run () = {}
        }

    // - - - - - - - - - - - - - - - - - - - - - - - -- - - - - - - - -  -- - - - - - - - -
    // Tests

    "StateUpdateScript" should {
        "not be equal to any other script of different class" in {
            scriptD ()  must_!=  scriptD2 (2)
        }

        "should be equal to any script instance of the same class" in {
            scriptD ()  must_==  scriptD ()
            scriptD2 (1)  must_==  scriptD2 (2)
            scriptD2 (1).hashCode  must_==  scriptD2 (2).hashCode
            (scriptD2 (1).y+1)  must_==  scriptD2 (2).y
        }

        "execute scripts with suspentions on some methods" in {
            scriptA.p1  must_==  0
            scriptA.p2  must_==  0
            scriptA.p3  must_==  0
            scriptA.p4  must_==  0

            scriptA.nextInstruction()  must_==  scriptA.SetState (true)

            scriptA.p1  must_==  1
            scriptA.p2  must_==  0
            scriptA.p3  must_==  0
            scriptA.p4  must_==  0

            scriptA.nextInstruction()  must_==  scriptA.Wait (1 minutes)

            scriptA.p1  must_==  1
            scriptA.p2  must_==  1
            scriptA.p3  must_==  0
            scriptA.p4  must_==  0

            scriptA.nextInstruction()  must_==  scriptA.SetState (false)

            scriptA.p1  must_==  1
            scriptA.p2  must_==  1
            scriptA.p3  must_==  1
            scriptA.p4  must_==  0

            scriptA.nextInstruction()  must_==  scriptA.End

            scriptA.p1  must_==  1
            scriptA.p2  must_==  1
            scriptA.p3  must_==  1
            scriptA.p4  must_==  1
        }

        "execute scripts with defaultOnInterrupt invokation if interrupted" in {
            scriptB.p1  must_==  0
            scriptB.p2  must_==  0
            scriptB.di  must_==  0

            scriptB.nextInstruction()  must_==  scriptB.SetState (true)

            scriptB.p1  must_==  1
            scriptB.p2  must_==  0
            scriptB.di  must_==  0

            scriptB.interrupt()

            scriptB.p1  must_==  1
            scriptB.p2  must_==  0
            scriptB.di  must_==  1
            scriptB.isInterrupted  must_==  true
        }

        "execute scripts with custom handler for invokation if interrupted" in {
            scriptC.p1  must_==  0
            scriptC.p2  must_==  0
            scriptC.di  must_==  0
            scriptC.i1  must_==  0

            scriptC.nextInstruction()  must_==  scriptC.SetState (true)

            scriptC.p1  must_==  1
            scriptC.p2  must_==  0
            scriptC.di  must_==  0
            scriptC.i1  must_==  0

            scriptC.interrupt()

            scriptC.p1  must_==  1
            scriptC.p2  must_==  0
            scriptC.di  must_==  0
            scriptC.i1  must_==  1
            scriptC.isInterrupted  must_==  true
        }
    }
}
