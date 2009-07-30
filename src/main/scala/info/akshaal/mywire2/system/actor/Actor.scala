package info.akshaal.mywire2
package system
package actor

import org.jetlang.fibers.{PoolFiberFactory, Fiber}
import org.jetlang.core.BatchExecutor

import Predefs._
import logger.Logging
import utils.{Pool, TimeUnit}
import system.RuntimeConstants
import scheduler.Scheduler

/**
 * Very simple and hopefully fast implementation of actors
 */
trait Actor extends Logging with NotNull {
    /** Pool to be used by actor. */
    protected val pool : Pool

    /** Scheduler to be used by actor */
    protected val scheduler : Scheduler

    /**
     * Implementing class is supposed to provide a body of the actor.
     */
    protected def act(): PartialFunction[Any, Unit]

    // ===================================================================
    // Concrete methods

    protected final val schedule = new ActorSchedule (this, scheduler)

    /**
     * Current sender. Only valid when act method is called.
     */
    protected var sender : Option[Actor] = None

    /**
     * A fiber used by this actor.
     */
    private[this] val fiber =
        new PoolFiberFactory (pool.executors).create (new ActorExecutor (this))

    /**
     * Send a message to the actor.
     */
    final def !(msg: Any): Unit = {
        val sentFrom = ThreadLocalState.current.get
        val runTimingFinisher = pool.latencyTiming.createFinisher

        // This runner will be executed by executor when time has come
        // to process the message
        val runner = mkRunnable {
            runTimingFinisher ("[latency] Actor started for message: " + msg)

            val executeTimingFinisher = pool.executionTiming.createFinisher

            // Execute            
            msg match {
                case Ping => sentFrom.foreach (_ ! Pong)
                case other => invokeAct (msg, sentFrom)
            }

            // Show complete latency
            executeTimingFinisher ("[execution] Actor completed for message: " + msg)
        }

        fiber.execute (runner)
    }

    /**
     * Invokes this actor's act() method.
     */
    private[this] def invokeAct (msg : Any, sentFrom : Option[Actor]) =
    {
        if (act.isDefinedAt (msg)) {
            sender = sentFrom

            logIgnoredException ("Error processing message: " + msg) {
                act () (msg)
            }

            sender = None
        } else {
            warn ("Ignored message: " + msg)
        }
    }

    /**
     * Start actor.
     */
    private[actor] final def start () = {
        debug ("About to start")
        fiber.start
    }

    /**
     * Stop the actor.
     */
    private[actor] final def stop() = {
        debug ("About to stop")
        fiber.dispose
    }
}

/**
 * Executor of queued actors.
 * @param actor this actor will be used as a current actor
 *        when processing messages of the actor.
 */
private[actor] class ActorExecutor (actor : Actor)
                extends BatchExecutor {
    final def execute (commands: Array[Runnable]) = {
        // Remember the current actor in thread local variable.
        // So later it may be referenced from ! method of other actors
        ThreadLocalState.current.set(Some(actor))

        // Execute
        for (command <- commands) {
            command.run
        }

        // Reset curren actor
        ThreadLocalState.current.set(None)
    }
}

/**
 * Thread local state of the actor environment.
 */
private[actor] object ThreadLocalState {
    final val current = new ThreadLocal[Option[Actor]]()
}
