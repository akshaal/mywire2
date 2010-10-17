/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package strategy

import info.akshaal.jacore.`package`._

import utils.stupdate.{StateUpdate, BooleanStateUpdate}

import java.util.Calendar

/**
 * Strategy interface for obtaining a state updates.
 *
 * @param T type of state
 */
trait Strategy [T] {
    /**
     * Calculate state.
     */
    def getStateUpdate () : StateUpdate[T]
}

/**
 * Simple strategy for a boolean state.
 *
 * @param onInternal interval to keep state as True (On)
 * @param offInternal interval to keep state as False (Off)
 * @param offset to shift intervals in time
 */
class SimpleOnOffStrategy (val onInterval : TimeValue,
                           val offInterval : TimeValue,
                           val offset : TimeValue = 0 nanoseconds)
                     extends Strategy [Boolean]
{
    private val cal = Calendar.getInstance ();
    private val offsetMs = cal.get (Calendar.ZONE_OFFSET) - offset.asMilliseconds
    private val onMs = onInterval.asMilliseconds
    private val offMs = offInterval.asMilliseconds
    private val periodMs = onMs + offMs

    override def getStateUpdate () : BooleanStateUpdate = {
        val currentMs = System.currentTimeMillis + offsetMs
        val doneMs = currentMs % periodMs
        val on = doneMs < onMs
        val delayMs = if (on) onMs - doneMs else periodMs - doneMs

        new BooleanStateUpdate (state = on, validTime = delayMs milliseconds);
    }
}