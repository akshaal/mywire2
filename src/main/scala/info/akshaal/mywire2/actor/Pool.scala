/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.actor

import info.akshaal.mywire2.Predefs._
import info.akshaal.mywire2.logger.Logging
import info.akshaal.mywire2.utils.{LatencyStat,
                                   ThreadPriorityChanger}

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}
import org.jetlang.core.BatchExecutor
import org.jetlang.fibers.{PoolFiberFactory, Fiber}

/**
 * Pool for low priority actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
trait LowPriorityPool {
    private[actor] final def createFiber(actor : MywireActor): Fiber =
        LowPriorityPool.create (actor)

    private[actor] val latency = LowPriorityPool.latency
}

/**
 * Pool for low prirority actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
trait HiPriorityPool {
    private[actor] final def createFiber(actor : MywireActor): Fiber =
        HiPriorityPool.create (actor)

    private[actor] val latency = HiPriorityPool.latency
}

/**
 * Pool itself for low priority actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
object LowPriorityPool extends Pool ("LowPriorityPool",
                                  ThreadPriorityChanger.LowPriority ())

/**
 * Pool for hi priority actors. Actors in this pool will be processed
 * as soon as possible.
 */
object HiPriorityPool extends Pool ("HiPriorityPool",
                                 ThreadPriorityChanger.HiPriority ())

/**
 * Pool class to be used by actors.
 */
private[actor] sealed class Pool (name : String,
                           priority : ThreadPriorityChanger.Priority) {
    private[actor] val latency = new LatencyStat

    private val numberOfThreadInPool =
    RuntimeConstants.threadsMultiplier * Runtime.getRuntime.availableProcessors

    private val threadFactory = new ThreadFactory {
        val counter = new AtomicInteger (0)

        def newThread (r : Runnable) : Thread = {
            val threadNumber = counter.incrementAndGet

            val proxy = mkRunnable {
                ThreadPriorityChanger.change (priority)
                r.run
            }

            val thread = new Thread (proxy)
            thread.setName (name + "-" + threadNumber)
            
            thread
        }
    }

    private val executors =
        Executors.newFixedThreadPool (numberOfThreadInPool, threadFactory);

    private val fiberFactory = new PoolFiberFactory (executors)

    private[actor] final def create (actor : MywireActor): Fiber =
        fiberFactory.create (new ActorExecutor (actor))

    final def getLatencyNano () = latency.getNano
}

/**
 * Executor of queued actors.
 */
private[actor] class ActorExecutor (actor : MywireActor) extends BatchExecutor {
    final def execute (commands: Array[Runnable]) = {
        // Remember the current actor in thread local variable.
        // So later it may be referenced from ! method of other actors
        ThreadLocalState.current.set(actor)

        // Execute
        for (command <- commands) {
            command.run
        }

        // Reset curren actor
        ThreadLocalState.current.set(null)
    }
}

/**
 * Thread local state of the actor environment.
 */
private[actor] object ThreadLocalState {
    val current = new ThreadLocal[MywireActor]()
}
