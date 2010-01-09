/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.daemon

import java.util.concurrent.Executors

/**
 * Holds references to objects that are usable only during bootstrap of daemon.
 */
object DaemonBoot {
    val executor = Executors.newCachedThreadPool ()
}
