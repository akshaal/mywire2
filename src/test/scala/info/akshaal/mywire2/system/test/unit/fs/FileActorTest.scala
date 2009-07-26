/*
 * ActorTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.test.unit.fs

import collection.immutable.List
import org.testng.annotations.Test
import org.testng.Assert._
import java.io.{File, FileReader, BufferedReader}

import mywire2.system.test.unit.{BaseUnitTest, UnitTestModule, HiPriorityActor}

import mywire2.system.fs.{WriteFile, WriteFileDone, WriteFileFailed}

class FileActorTest extends BaseUnitTest {
    @Test (groups=Array("unit"))
    def testWrite () = {
        UnitTestModule.ActorManagerImpl.startActor (WriteTestActor)

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

        UnitTestModule.ActorManagerImpl.stopActor (WriteTestActor)
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
            UnitTestModule.FileActorImpl ! (WriteFile (file, content))
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
