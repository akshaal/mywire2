/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.unit.actor

import collection.immutable.List
import org.testng.annotations.Test
import org.testng.Assert._

import test.common.BaseTest
import mywire2.system.test.TestHelper
import mywire2.system.actor.{HiPriorityActor, LowPriorityActor}

class ActorTest extends BaseTest {
    @Test (groups=Array("indie"))
    def testPingPong () = {
        SampleActor ! 1
        TestHelper.startActor (SampleActor)
        TestHelper.startActor (ToStringActor)
        SampleActor ! 3
        SampleActor ! 7
        sleep ()
        TestHelper.exitActor (SampleActor)
        TestHelper.exitActor (ToStringActor)

        assertEquals (SampleActor.accuInt, List(7, 3, 1))
        assertEquals (SampleActor.accuString, List("x7", "x3", "x1"))
        assertTrue (TestHelper.getHiPriorityPoolLatency > 0, "latnecy cannot be 0")
        assertTrue (TestHelper.getLowPriorityPoolLatency > 0, "latnecy cannot be 0")

        debug ("current latency of hiPriorityPool = "
               + TestHelper.getHiPriorityPoolLatency)

        debug ("current latency of LowPriorityPool = "
               + TestHelper.getLowPriorityPoolLatency)
    }

    @Test (groups=Array("indie"))
    def testExceptionResistance () = {
        TestHelper.startActor (UnstableActor)

        for (i <- 1 to 10) {
            UnstableActor ! i
        }

        sleep
        TestHelper.exitActor (UnstableActor)

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

object ToStringActor extends LowPriorityActor {
    def act () = {
        case x => {
            debug ("Received message: " + x)
            sender ! ("x" + x);
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
