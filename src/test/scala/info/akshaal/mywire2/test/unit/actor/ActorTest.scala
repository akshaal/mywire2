/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.unit.actor

import org.testng.annotations.Test
import org.testng.Assert._

import info.akshaal.mywire2.test.common.BaseTest
import info.akshaal.mywire2.logger.Logging
import info.akshaal.mywire2.actor.{MywireActor, HiSpeedPool, LowSpeedPool}

@Test (groups=Array("simple"))
class ActorTest extends BaseTest {
    @Test def testPingPong () = {
        SampleActor ! 1
        SampleActor.start
        ToStringActor.start
        SampleActor ! 3
        SampleActor ! 7
        sleep ()
        SampleActor.exit
        ToStringActor.exit

        assertEquals (SampleActor.accuInt, List(7, 3, 1))
        assertEquals (SampleActor.accuString, List("x7", "x3", "x1"))
        assertTrue (HiSpeedPool.getLatencyNano () > 0, "latnecy cannot be 0")
        assertTrue (LowSpeedPool.getLatencyNano () > 0, "latnecy cannot be 0")

        debug ("current latency of hiSpeedPool = "
               + HiSpeedPool.getLatencyNano ())

        debug ("current latency of LowSpeedPool = "
               + LowSpeedPool.getLatencyNano ())
    }

    @Test def testExceptionResistance () = {
        UnstableActor.start

        for (i <- 1 to 10) {
            UnstableActor ! i
        }

        sleep
        UnstableActor.exit

        assertEquals (UnstableActor.sum, 1+3+5+7+9)
    }

    private def sleep () = Thread.sleep (1000)
}

object SampleActor extends MywireActor with HiSpeedPool {
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

object ToStringActor extends MywireActor with LowSpeedPool {
    def act () = {
        case x => {
            debug ("Received message: " + x)
            sender ! ("x" + x);
        }
    }
}

object UnstableActor extends MywireActor with HiSpeedPool {
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