/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system.test.unit.actor

import collection.immutable.List
import org.testng.annotations.Test
import org.testng.Assert._

import system.test.unit.{BaseUnitTest, UnitTestModule, HiPriorityActor}

class ActorTest extends BaseUnitTest {
    @Test (groups=Array("unit"))
    def testPingPong () = {
        SampleActor ! 1
        UnitTestModule.actorManager.startActor (SampleActor)
        UnitTestModule.actorManager.startActor (ToStringActor)
        SampleActor ! 3
        SampleActor ! 7
        sleep ()
        UnitTestModule.actorManager.stopActor (SampleActor)
        UnitTestModule.actorManager.stopActor (ToStringActor)

        assertEquals (SampleActor.accuInt, List(7, 3, 1))
        assertEquals (SampleActor.accuString, List("x7", "x3", "x1"))
    }

    @Test (groups=Array("unit"))
    def testExceptionResistance () = {
        UnitTestModule.actorManager.startActor (UnstableActor)

        for (i <- 1 to 10) {
            UnstableActor ! i
        }

        sleep
        UnitTestModule.actorManager.stopActor (UnstableActor)

        assertEquals (UnstableActor.sum, 1+3+5+7+9)
    }

    private def sleep () = Thread.sleep (1000)
}

object SampleActor extends HiPriorityActor {
    var accuString : List[String] = Nil
    var accuInt : List[Int] = Nil

    def act () = {
        case x : Int => {
            debug ("Received [Int] message: " + x)
            accuInt = x :: accuInt
            ToStringActor ! x
        }

        case x : String => {
            debug ("Received [String] message: " + x)
            accuString = x :: accuString
        }
    }
}

object ToStringActor extends HiPriorityActor {
    def act () = {
        case x => {
            debug ("Received message: " + x)
            sender.foreach(_ ! ("x" + x))
        }
    }
}

object UnstableActor extends HiPriorityActor {
    var sum = 0

    def act () = {
        case x : Int => {
            if (x % 2 == 0) {
                throw new IllegalArgumentException ()
            } else {
                sum += x
            }
        }
    }
}