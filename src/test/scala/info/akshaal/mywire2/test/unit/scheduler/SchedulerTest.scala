/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.unit.actor

import org.testng.annotations.Test
import org.testng.Assert._

import info.akshaal.mywire2.scheduler.Scheduler
import info.akshaal.mywire2.test.common.BaseTest
import info.akshaal.mywire2.actor.{MywireActor, HiSpeedPool}

class SchedulerTest extends BaseTest {
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
    }    
}

object OneTimeTestActor extends MywireActor with HiSpeedPool {
    var executed = false

    def act () = {
        case x : Int => {
            debug ("Received [Int] message: " + x)
            executed = true
        }
    }
}

object OneTimeTestActor2 extends MywireActor with HiSpeedPool {
    var executed = false

    def act () = {
        case x : Int => {
            debug ("Received [Int] message: " + x)
            executed = true
        }
    }
}