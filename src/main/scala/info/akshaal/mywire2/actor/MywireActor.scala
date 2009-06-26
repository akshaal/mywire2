package info.akshaal.mywire2.actor

import java.util.concurrent.Executors
import org.jetlang.core.BatchExecutor
import org.jetlang.fibers.{PoolFiberFactory, Fiber}

// TODO: Make it exceptions resitant!

/**
 * Very simple and hopefully fast implementation of actors
 */
trait MywireActor {
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
     * Send a message to the actor.
     */
    sealed def !(msg: Any): Unit = {
        val sentFrom = ThreadLocalState.current.get

        val runner = new Runnable() {
            def run() = {
                if (act.isDefinedAt (msg)) {
                    sender = sentFrom
                    act () (msg)
                    sender = null
                }
            }
        }

        fiber.execute (runner)
    }

    /**
     * Start this actor.
     */
    sealed def start() = fiber.start

    /**
     * Stop the actor.
     */
    sealed def exit() = fiber.dispose
}

/**
 * Pool for low speed actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
trait LowSpeedPool {
    private[actor] def createFiber(actor : MywireActor): Fiber =
        LowSpeedPool.create (actor)
}

/**
 * Pool for low speed actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
trait HiSpeedPool {
    private[actor] def createFiber(actor : MywireActor): Fiber =
        HiSpeedPool.create (actor)
}

/**
 * Pool itself for low speed actors. Actors in this pool will be processed
 * when there is no other important task to do.
 */
private[actor] object LowSpeedPool extends Pool

/**
 * Pool for hi speed actors. Actors in this pool will be processed
 * as soon as possible.
 */
private[actor] object HiSpeedPool extends Pool

/**
 * Pool class to be used by actors.
 */
private[actor] class Pool {
    private val numberOfThreadInPool =
        2 * Runtime.getRuntime.availableProcessors

    private val executors =
        Executors.newFixedThreadPool (numberOfThreadInPool);

    private val fiberFactory = new PoolFiberFactory (executors)

    private[actor] def create (actor : MywireActor): Fiber =
        fiberFactory.create (new ActorExecutor (actor))
}

/**
 * Executor of queued actors.
 */
private[actor] class ActorExecutor (actor : MywireActor) extends BatchExecutor {
    def execute (commands: Array[Runnable]) = {
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
