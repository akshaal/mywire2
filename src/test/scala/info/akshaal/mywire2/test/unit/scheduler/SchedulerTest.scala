/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.unit.actor

import org.testng.annotations.Test
import org.testng.Assert._

import info.akshaal.mywire2.scheduler.{Scheduler, TimeOut}
import info.akshaal.mywire2.test.common.BaseTest
import info.akshaal.mywire2.actor.{MywireActor, HiSpeedPool}

class SchedulerTest extends BaseTest {
    @Test (groups=Array("scheduler"))
    def testRecurrentScheduling () = {
        RecurrentTestActor.start ()

        RecurrentTestActor.invocations = 0
        Thread.sleep (400)

        assertTrue (RecurrentTestActor.invocations >= 6,
                    "After 400ms, RecurrentTestActor should be executed at least 6 times")
        assertTrue (RecurrentTestActor.invocations <= 10,
                    "After 400ms, RecurrentTestActor should be executed 10 at the most")

        Thread.sleep (400)

        assertTrue (RecurrentTestActor.invocations >= 14,
                    "After 800ms, RecurrentTestActor should be executed at least 14 times")
        assertTrue (RecurrentTestActor.invocations <= 18,
                    "After 800ms, RecurrentTestActor should be executed 18 at the most")

        RecurrentTestActor.exit ()
    }

    @Test (groups=Array("scheduler"))
    def testOneTimeScheduling () = {
        OneTimeTestActor.start
        OneTimeTestActor2.start

        Scheduler.inMili (OneTimeTestActor, 123, 100)
        Scheduler.inMili (OneTimeTestActor2, 234, 50)

        Thread.sleep (30)

        assertFalse (OneTimeTestActor.executed,
                     "Actor must not be executet at this point")

        assertFalse (OneTimeTestActor2.executed,
                     "Actor must not be executet at this point")

        Thread.sleep (40)

        assertFalse (OneTimeTestActor.executed,
                     "Actor must not be executet at this point")

        assertTrue (OneTimeTestActor2.executed,
                    "Actor 2 must be executed at this point")

        Thread.sleep (200)

        assertTrue (OneTimeTestActor.executed,
                    "Actor must be executed at this point")

        assertTrue (OneTimeTestActor2.executed,
                    "Actor must be executed at this point")

        OneTimeTestActor.exit
        OneTimeTestActor2.exit

        debug ("Scheduler latency " + Scheduler.getLatencyNano)
    }    
}

object OneTimeTestActor extends MywireActor with HiSpeedPool {
    var executed = false

    def act () = {
        case TimeOut (x : Int) => {
            debug ("Received [Int] message: " + x)
            executed = true
        }
    }
}

object OneTimeTestActor2 extends MywireActor with HiSpeedPool {
    var executed = false

    def act () = {
        case TimeOut (x : Int) => {
            debug ("Received [Int] message: " + x)
            executed = true
        }
    }
}

object RecurrentTestActor extends MywireActor with HiSpeedPool {
    schedule payload "Hi" every 50 miliseconds
    
    var invocations = 0

    def act () = {
        case TimeOut (x : String) => {
            debug ("Received message: " + x)
            invocations += 1
        }
    }
}