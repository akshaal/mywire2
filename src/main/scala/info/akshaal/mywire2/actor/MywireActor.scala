package info.akshaal.mywire2.actor

import info.akshaal.mywire2.logger.{Logger, LogActor}
import info.akshaal.mywire2.utils.{LatencyStat,
                                   ThreadPriorityChanger}

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}
import org.jetlang.core.BatchExecutor
import org.jetlang.fibers.{PoolFiberFactory, Fiber}

/**
 * Very simple and hopefully fast implementation of actors
 */
trait MywireActor {
    private val logger = Logger.get (this)

    /**
     * A fiber used by this actor.
     */
    private val fiber = createFiber (this)

    /**
     * Current sender. Only valid when act method is called.
     */
    protected var sender : MywireActor = null

    /**
     * Implementing class is supposed to provide a body of the actor.
     */
    protected def act(): PartialFunction[Any, Unit]

    /**
     * Must be provided to create a fiber.
     */
    private[actor] def createFiber (actor : MywireActor): Fiber

    /**
     * Latency to be used for latency measurements.
     */
    private[actor] val latency : LatencyStat

    /**
     * Send a message to the actor.
     */
    final def !(msg: Any): Unit = {
        val sentFrom = ThreadLocalState.current.get
        val expectation = latency.expectationInNano(0)

        val runner = new Runnable() {
            def run() = {
                doAct (sentFrom, expectation, msg)
            }
        }

        fiber.execute (runner)
    }

    /**
     * Actually run act method with some decorations...
     */
    def doAct (sentFrom : MywireActor,
               expectation : Long,
               msg : Any) = {
        latency.measure(expectation)

        if (act.isDefinedAt (msg)) {
            // Defined

            sender = sentFrom

            try {
                act () (msg)
            } catch {
                case ex: Exception => {
                    if (MywireActor.this == LogActor) {
                        ex.printStackTrace
                    } else {
                        logger.error ("Exception in actor"
                                      + " while processing message: "
                                      + msg,
                                      ex)
                    }
                }
            }

            sender = null
        } else {
            // Not defined
            logger.warn ("Actor ignored the message: " + msg)
        }
    }

    /**
     * Start this actor.
     */
    final def start() = fiber.start

    /**
     * Stop the actor.
     */
    final def exit() = fiber.dispose
}

/**
 * Pool for low speed actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
trait LowSpeedPool {
    private[actor] final def createFiber(actor : MywireActor): Fiber =
        LowSpeedPool.create (actor)

    private[actor] val latency = LowSpeedPool.latency
}

/**
 * Pool for low speed actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
trait HiSpeedPool {
    private[actor] final def createFiber(actor : MywireActor): Fiber =
        HiSpeedPool.create (actor)

    private[actor] val latency = HiSpeedPool.latency
}

/**
 * Pool itself for low speed actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
object LowSpeedPool extends Pool ("LowSpeedPool",
                                  ThreadPriorityChanger.LowPriority ())

/**
 * Pool for hi speed actors. Actors in this pool will be processed
 * as soon as possible.
 */
object HiSpeedPool extends Pool ("HiSpeedPool",
                                 ThreadPriorityChanger.HiPriority ())

/**
 * Pool class to be used by actors.
 */
private[actor] class Pool (name : String,
                           priority : ThreadPriorityChanger.Priority) {
    private val logger = Logger.get

    private[actor] val latency = new LatencyStat

    private val numberOfThreadInPool =
    RuntimeConstants.threadsMultiplier * Runtime.getRuntime.availableProcessors

    private val threadFactory = new ThreadFactory {
        val counter = new AtomicInteger (0)

        def newThread (r : Runnable) : Thread = {
            val threadNumber = counter.incrementAndGet

            val proxy = new Runnable () {
                def run () = {
                    ThreadPriorityChanger.change (priority)
                    r.run
                }
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
