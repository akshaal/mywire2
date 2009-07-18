/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.daemon

import collection.immutable.{List, Nil}
import system.RuntimeConstants
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

import logger.{Logging, DummyLogging}

/**
 * Daemon.
 */
class Daemon {
    /**
     * Called by native executable to initialize the application before starting it.
     */
    def init () = ()

    /**
     * Called by native executable to start the application after the application
     * has been initialized.
     */
    def start () = ()

    /**
     * Called by native executable to stop the application before destroying it.
     */
    def stop () = ()

    /**
     * Called by native executable to destroy the application.
     */
    def destroy () = ()
}

/**
 * Util methods for the daemon.
 */
object Daemon extends DummyLogging {
    private var dying = false

    def isDying () = dying 

    /**
     * Called when application is no more reliable and must die.
     */
    def die () : Unit = {
        // Don't die twice
        synchronized {
            if (dying) {
                error ("Already dying. A request for dying is ignored")
                return
            }

            dying = true
        }

        // Dying
        error ("About to die, but first... postmortum information:")
        dumpThreads
    }

    /**
     * Dump threads stack.
     */
    private def dumpThreads () = {
        val traces : Map[Thread, Array[StackTraceElement]] = Thread.getAllStackTraces ()

        var threadDumpList : List[String] = Nil
        for ((thread, stackTraceElements) <- traces) {
            val name = thread.getName
            val stack = stackTraceElements.mkString (",\n    ")

            threadDumpList = (name + ":\n    " + stack + "\n") :: threadDumpList
        }

        error ("Dumping threads:\n" + threadDumpList.mkString ("\n"))
    }
}