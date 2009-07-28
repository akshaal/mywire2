/*
 * DeamonStatus.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.daemon

import mywire2.Predefs._
import logger.DummyLogging
import jmx.{SimpleJmx, JmxAttr, JmxOper}

import collection.immutable.{List, Nil}
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

private[system] abstract class DaemonStatus
                extends DummyLogging with SimpleJmx
{
    @volatile
    private var shuttingDown = false

    @volatile
    private var dying = false
    
    @volatile
    private var lastAliveTimestamp = System.nanoTime.nanoseconds

    /**
     * List of exposed JMX attributes.
     */
    override lazy val jmxAttributes = List (
        JmxAttr ("dying",           Some (() => dying),          None),
        JmxAttr ("shuttingDown",    Some (() => shuttingDown),   None)
    )

    /**
     * List of exposed JMX operations.
     */
    override lazy val jmxOperations = List (
        JmxOper ("shutdown", () => shutdown ())
    )

    /**
     * Returns true if application is dying (feels bad).
     */
    final def isDying = dying

    /**
     * Returns true if the application is shutting down.
     */
    final def isShuttingDown = shuttingDown

    /**
     * Returns timestamp when the application was alive last time.
     */
    final def lastAlive = lastAliveTimestamp

    /**
     * Called by monitoring actor to set
     */
    final def monitoringAlive () = lastAliveTimestamp = System.nanoTime.nanoseconds

    /**
     * Called when application is no more reliable and must die.
     */
    final def die () : Unit = {
        // Don't die twice
        synchronized {
            if (dying) {
                error ("Already dying. A request for dying is ignored")
                return
            }

            dying = true
        }

        // Dying
        error ("Soon will die, but first... postmortum information:")
        dumpThreads
        
        // Shutdown gracefully if possible
        shutdown
    }

    /**
     * Called when shutdown is requested.
     */
    final def shutdown () : Unit = {
        synchronized {
            if (!shuttingDown) {
                info ("Shutdown requested. Shutting down...")
                shuttingDown = true
            }
        }
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