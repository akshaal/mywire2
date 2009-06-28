/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.unit.actor

import org.testng.annotations.Test
import org.testng.Assert._

import info.akshaal.mywire2.logger.Logger
import info.akshaal.mywire2.actor.{MywireActor, HiSpeedPool, LowSpeedPool}

@Test (groups=Array("simple"))
class ActorTest  {
    private val logger = Logger.get

    @Test def testPingPong () = {
        SampleActor ! 1
        SampleActor.start
        ToStringActor.start
        SampleActor ! 3
        SampleActor ! 7
        sleep ()
        SampleActor.exit
        ToStringActor.exit

        assertEquals (List(7, 3, 1), SampleActor.accuInt)
        assertEquals (List("x7", "x3", "x1"), SampleActor.accuString)
        assertTrue (HiSpeedPool.getLatencyNano () > 0, "latnecy cannot be 0")
        assertTrue (LowSpeedPool.getLatencyNano () > 0, "latnecy cannot be 0")

        logger.debug ("current latency of hiSpeedPool = "
                      + HiSpeedPool.getLatencyNano ())

        logger.debug ("current latency of LowSpeedPool = "
                      + LowSpeedPool.getLatencyNano ())
    }
    
    private def sleep () = Thread.sleep (1000)
}

object SampleActor extends MywireActor with HiSpeedPool {
    private val logger = Logger.get
    var accuString : List[String] = Nil
    var accuInt : List[Int] = Nil

    def act () = {
        case x : Int => {
            logger.debug ("Received [Int] message: " + x)
            accuInt = x :: accuInt
            ToStringActor ! x
        }

        case x : String => {
            logger.debug ("Received [String] message: " + x)
            accuString = x :: accuString
        }
    }
}

object ToStringActor extends MywireActor with LowSpeedPool {
    private val logger = Logger.get

    def act () = {
        case x => {
            logger.debug ("Received message: " + x)
            sender ! ("x" + x);
        }
    }
}