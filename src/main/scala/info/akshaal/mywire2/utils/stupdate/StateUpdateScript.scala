/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils
package stupdate

import scala.util.continuations._

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.logger.Logging

/**
 * Asynchronous script to update state in a sequence of operations.
 *
 * This works as follows. A programmer is supposed to subclass this class providing
 * implementation of run method. The instance of the subclass represents an instance of the script.
 * The first call to the nextInstruction method, runs script. As soon as a suspending
 * method is called from inside of run method, the execution is captured into the continuation
 * and stored for execution in future and the nextInstruction returns code
 * of operation that suspsended execution. As soon as program finished doing some job
 * specified by instruction, the application calls nextInstruction again to resume
 * execution of continuation.
 *
 * Such approach helps to keep scripts simple and non-blocking.
 *
 * @tparam T type of state.
 */
abstract class StateUpdateScript[T] extends AbstractStateUpdate[T] with Logging {
    // Overral state types of script
    private sealed abstract class ScriptState
    private case object NotStarted extends ScriptState
    private case class Suspended (cont : Unit => Unit) extends ScriptState
    private case object Finished extends ScriptState
    private case object Interrupted extends ScriptState

    // Instructions produced by script
    private[mywire2] abstract sealed class Instruction
    private[mywire2] case object Nop extends Instruction
    private[mywire2] case class SetState (state : T) extends Instruction
    private[mywire2] case class Wait (time : TimeValue) extends Instruction

    // Current state of script
    private var scriptState : ScriptState = NotStarted
    private var lastStepResult : Instruction = Nop
    private var onInterrupt = () => {defaultOnInterrupt}

    /**
     * Run or continue script execution and return state update.
     * @return next Instruction
     */
    private[mywire2] def nextInstruction () : Instruction = {
        // By default instruction is Nop, but may be overriden in the script
        lastStepResult = Nop

        // Execute script
        scriptState match {
            case Finished =>
                warn ("The script is already finished execution!")

            case Interrupted =>
                warn ("The script is already interrupted!")

            case NotStarted =>
                reset {
                    run ()
                }

            case Suspended (cont) =>
                cont ()
        }

        // If instruction is Nop then script has finished its job
        if (lastStepResult == Nop) {
            scriptState = Finished
        }

        // Return result
        lastStepResult
    }

    /**
     * Interrupt execution of script. Call to this method triggers execution of
     * scripts onInterrupt callback and invalidates script state.
     */
    private[mywire2] def interrupt () : Unit = {
        this.scriptState = Interrupted
        onInterrupt ()
    }

    /**
     * This method is invoked in order to evaluate script.
     * This is where script's body must be defined.
     */
    protected def run () : Unit @suspendable

    /**
     * Suspend current execution. The result of suspension is instruction.
     */
    private def suspend (instruction : Instruction, onInterrupt : () => Unit) : Unit @suspendable = {
        shift ((k : (Unit => Unit)) => {
            lastStepResult = instruction
            this.onInterrupt = onInterrupt
            scriptState = Suspended (k)
        })
    }

    /**
     * Default callback for interrupt during script execution.
     */
    protected def defaultOnInterrupt () {
    }

    // =================================================================================
    // Methods to be used from run method.

    /**
     * Set new state.
     * This method is to be called from run method.
     */
    protected final def set (state : T) : Unit @suspendable = {
        setOrFail (state) {defaultOnInterrupt}
    }

    /**
     * Set new state.
     * This method is to be called from run method.
     */
    protected final def setOrFail (state : T) (onInterrupt : => Unit) : Unit @suspendable = {
        suspend (SetState (state), () => onInterrupt)
    }

    /**
     * Wait for the given time. This is safe to use in rum method, because
     * this method will not block the thread.
     * This method is to be called from run method only.
     */
    protected final def wait (time : TimeValue) : Unit @suspendable = {
        waitOrFail (time) {defaultOnInterrupt}
    }

    /**
     * Wait for the given time. This is safe to use in rum method, because
     * this method will not block the thread.
     * This method is to be called from run method only.
     */
    protected final def waitOrFail (time : TimeValue) (onInterrupt : => Unit) : Unit @suspendable = {
        suspend (Wait (time), () => onInterrupt)
    }
}
