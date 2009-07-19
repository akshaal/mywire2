/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.unit.fs

import collection.immutable.List
import org.testng.annotations.Test
import org.testng.Assert._
import java.io.{File, FileReader, BufferedReader}

import test.common.BaseTest
import mywire2.system.test.TestHelper
import mywire2.system.actor.HiPriorityActor
import mywire2.system.fs.{FileActor, WriteFile, WriteFileDone, WriteFileFailed}

class FileActorTest extends BaseTest {
    @Test (groups=Array("indie"))
    def testWrite () = {
        TestHelper.startActor (WriteTestActor)

        val file = File.createTempFile ("mywire2", "test")
        file.deleteOnExit

        WriteTestActor ! (file, "Hi")

        sleep ()
        assertEquals (readLine (file), "Hi")
        assertEquals (WriteTestActor.done, 1)
        assertEquals (WriteTestActor.excs, 0)

        WriteTestActor ! (file, "Bye")
        sleep ()
        assertEquals (readLine (file), "Bye")
        assertEquals (WriteTestActor.done, 2)
        assertEquals (WriteTestActor.excs, 0)

        WriteTestActor ! (new File ("/oops/oops/ooopsss!!"), "Ooops")
        sleep ()
        assertEquals (WriteTestActor.done, 2)
        assertEquals (WriteTestActor.excs, 1)

        TestHelper.exitActor (WriteTestActor)

        // TODO test exceptions while writing
    }

    private def sleep () = Thread.sleep (1000)

    private def readLine (file : File) : String = {
        val reader = new BufferedReader (new FileReader (file))
        try {
            reader.readLine()
        } finally {
            reader.close ()
        }
    }
}

object WriteTestActor extends HiPriorityActor {
    var done = 0
    var excs = 0

    def act () = {
        case msg @ (file : File, content : String) => {
            debug ("Received message: " + msg)
            TestHelper.fileActor ! (WriteFile (file, content))
        }

        case msg @ WriteFileDone (file) => {
            done = done + 1
            debug ("Received message: " + msg)
        }

        case msg @ WriteFileFailed (file, exc) => {
            excs = excs + 1
            debug ("Received message: " + msg)
        }
    }
}
