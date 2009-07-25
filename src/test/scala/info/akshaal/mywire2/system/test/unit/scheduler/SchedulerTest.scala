/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.test.unit.actor

import org.testng.annotations.Test
import org.testng.Assert._

import mywire2.Predefs._
import mywire2.system.scheduler.TimeOut

import mywire2.system.test.unit.{BaseUnitTest, UnitTestModule, HiPriorityActor}

class SchedulerTest extends BaseUnitTest {
    @Test (groups=Array("indie"))
    def testRecurrentScheduling () = {
        UnitTestModule.ActorManagerImpl.startActor (RecurrentTestActor)

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

        UnitTestModule.ActorManagerImpl.stopActor (RecurrentTestActor)
    }

    @Test (groups=Array("indie"))
    def testOneTimeScheduling () = {
        UnitTestModule.ActorManagerImpl.startActor (OneTimeTestActor)
        UnitTestModule.ActorManagerImpl.startActor (OneTimeTestActor2)

        UnitTestModule.SchedulerImpl.in (OneTimeTestActor, 123, 130.milliseconds)
        UnitTestModule.SchedulerImpl.in (OneTimeTestActor2, 234, 50.milliseconds)

        Thread.sleep (30)

        assertFalse (OneTimeTestActor.executed,
                     "Actor must not be executet at this point")

        assertFalse (OneTimeTestActor2.executed,
                     "Actor must not be executet at this point")

        Thread.sleep (60)

        assertFalse (OneTimeTestActor.executed,
                     "Actor must not be executet at this point")

        assertTrue (OneTimeTestActor2.executed,
                    "Actor 2 must be executed at this point")

        Thread.sleep (200)

        assertTrue (OneTimeTestActor.executed,
                    "Actor must be executed at this point")

        assertTrue (OneTimeTestActor2.executed,
                    "Actor must be executed at this point")

        UnitTestModule.ActorManagerImpl.stopActor (OneTimeTestActor)
        UnitTestModule.ActorManagerImpl.stopActor (OneTimeTestActor2)
    }    
}

object OneTimeTestActor extends HiPriorityActor {
    var executed = false

    def act () = {
        case TimeOut (x : Int) => {
            debug ("Received [Int] message: " + x)
            executed = true
        }
    }
}

object OneTimeTestActor2 extends HiPriorityActor {
    var executed = false

    def act () = {
        case TimeOut (x : Int) => {
            debug ("Received [Int] message: " + x)
            executed = true
        }
    }
}

object RecurrentTestActor extends HiPriorityActor {
    schedule payload "Hi" every 50 milliseconds
    
    var invocations = 0

    def act () = {
        case TimeOut (x : String) => {
            debug ("Received message: " + x)
            invocations += 1
        }
    }
}
