/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.unit.actor

import org.testng.annotations.Test;
import info.akshaal.mywire2.logger.Logger
import info.akshaal.mywire2.actor.{MywireActor, HiSpeedPool, LowSpeedPool}

@Test {val groups=Array("simple")}
class ActorTest {
    private val logger = Logger.get

    @Test def testPingPong () = {
        PingActor.start
        // TODO: Implement something clever
        sleep ()
        PingActor.exit
    }
    
    private def sleep () = Thread.sleep (1000)
}

object PingActor extends MywireActor with HiSpeedPool {
    private val logger = Logger.get

    def act () = {
        case x => logger.info ("Received message: " + x + ", sender=" + sender)
    }
}