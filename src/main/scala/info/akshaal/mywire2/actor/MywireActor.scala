package info.akshaal.mywire2.actor

import info.akshaal.mywire2.Predefs._
import info.akshaal.mywire2.logger.Logging
import info.akshaal.mywire2.utils.LatencyStat

import org.jetlang.fibers.Fiber

/**
 * Very simple and hopefully fast implementation of actors
 */
trait MywireActor extends Logging {
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
        val runExpectation = LatencyStat.expectationInNano (0)

        val runner = mkRunnable {
            val runLatency = latency.measureNano (runExpectation)
            debugLazy ("Actor's latency for processing message " + msg
                       + " is " + runLatency + " ns")
            if (runLatency > RuntimeConstants.warnLatencyNano) {
                warn ("Actor's latency for processing message " + msg
                       + " is " + runLatency + " ns")
            }

            // TODO: Show warn about run latency if exceeds

            val completeExpectation = LatencyStat.expectationInNano (0)

            if (act.isDefinedAt (msg)) {
                // Defined

                sender = sentFrom

                logIgnoredException (logger,
                                     "Exception in actor while processing message: "
                                     + msg) {
                    act () (msg)
                }

                sender = null
            } else {
                // Not defined
                warn ("Actor ignored the message: " + msg)
            }

            // TODO: Show warn if exceeds
            val completeLatency =
                LatencyStat.calculateLatencyNano (completeExpectation)

            debugLazy ("Actor completed processing message " + msg
                       + " in " + completeLatency + " ns")
            if (completeLatency > RuntimeConstants.warnActorTimeNano) {
                warn ("Actor completed processing message " + msg
                       + " in " + completeLatency + " ns")
            }
        }

        fiber.execute (runner)
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

