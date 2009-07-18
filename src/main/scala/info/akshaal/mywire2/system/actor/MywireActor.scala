package info.akshaal.mywire2.actor

import info.akshaal.mywire2.Predefs._
import info.akshaal.mywire2.logger.Logging
import info.akshaal.mywire2.utils.{LatencyStat,
                                   HiPriorityPool,
                                   LowPriorityPool,
                                   Pool}

import org.jetlang.fibers.{PoolFiberFactory, Fiber}
import org.jetlang.core.BatchExecutor

/**
 * Low priority actor.
 */
abstract class LowPriorityActor extends Actor (LowPriorityPool)

/**
 * Hi priority actor.
 */
abstract class HiPriorityActor extends Actor (HiPriorityPool)

/**
 * Very simple and hopefully fast implementation of actors
 */
abstract class Actor (pool : Pool) extends Logging {
    protected final val schedule = new ActorSchedule (this)

    /**
     * Implementing class is supposed to provide a body of the actor.
     */
    protected def act(): PartialFunction[Any, Unit]

    /**
     * A fiber used by this actor.
     */
    private val fiber =
        new PoolFiberFactory (pool.executors).create (new ActorExecutor (this))

    /**
     * Current sender. Only valid when act method is called.
     */
    protected var sender : Actor = null

    /**
     * Latency.
     */
    private val latency = pool.latency

    /**
     * Send a message to the actor.
     */
    final def !(msg: Any): Unit = {
        val sentFrom = ThreadLocalState.current.get
        val runExpectation = LatencyStat.expectationInNano (0)

        val runner = mkRunnable {
            // Show run latency
            val runLatency = latency.measureNano (runExpectation)
            LatencyStat.inform (logger,
                                "Actor started for message: " + msg,
                                RuntimeConstants.warnLatency.asNanoseconds,
                                runLatency)

            val completeExpectation = LatencyStat.expectationInNano (0)

            // Execute            
            msg match {
                case Ping => sentFrom ! Pong
                case other => invokeAct (msg, sentFrom)
            }

            // Show complete latency
            val completeLatency =
                LatencyStat.calculateLatencyNano (completeExpectation)
            
            LatencyStat.inform (logger,
                                "Actor completed for message: " + msg,
                                RuntimeConstants.warnActorTime.asNanoseconds,
                                completeLatency)
        }

        fiber.execute (runner)
    }

    private def invokeAct (msg : Any, sentFrom : Actor) = {
        if (act.isDefinedAt (msg)) {
            // Defined

            sender = sentFrom

            logIgnoredException (logger,
                                 "Exception in actor while processing message: "
                                 + msg)
            {
                act () (msg)
            }

            sender = null
        } else {
            // Not defined
            warn ("Actor ignored the message: " + msg)
        }
    }

    /**
     * Start this actor.
     */
    final def startSkippingMonitoring = {
        debug ("About to start")
        fiber.start
    }

    final def start () = {
        startSkippingMonitoring
        Monitoring.add (this)
    }

    /**
     * Stop the actor.
     */
    final def exit() = {
        debug ("About to stop")
        fiber.dispose
        Monitoring.remove (this)
    }
}

/**
 * Executor of queued actors.
 */
private[actor] class ActorExecutor (actor : Actor) extends BatchExecutor {
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
    val current = new ThreadLocal[Actor]()
}
