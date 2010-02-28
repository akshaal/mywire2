/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package daemon

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.Actor

/**
 * Actor marked with this interface are supposed to be started automatically by daemon
 * if daemon knows what packages scan for.
 */
trait Autostart {
    this : Actor =>
}
