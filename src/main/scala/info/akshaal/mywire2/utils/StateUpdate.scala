/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

import info.akshaal.jacore.`package`._

/**
 * Update for a state. Used to keep state update with additional information.
 * @param T type of state
 * @param state new state
 * @param validTime how this state is valid
 */
class StateUpdate[T] (val state : T, val validTime : TimeValue)