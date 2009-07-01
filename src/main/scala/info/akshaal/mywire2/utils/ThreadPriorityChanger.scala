/*
 * newObject.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

import info.akshaal.mywire2.RuntimeConstants
import info.akshaal.mywire2.logger.Logging

import ru.toril.daemonhelper.{DaemonHelper, OSException}

object ThreadPriorityChanger extends Logging {
    abstract sealed class Priority
    case class LowPriority extends Priority
    case class HiPriority extends Priority

    def change (priority : Priority) = {
        val pid = DaemonHelper.getPid
        val tid = DaemonHelper.getTid
        val name = Thread.currentThread.getName
        val identifier = name + " (pid=" + pid + ", tid=" + tid + ")"
        val osPriority =
            priority match {
                case LowPriority () =>
                    RuntimeConstants.lowPriorityThreadOSPriority

                case HiPriority () =>
                    RuntimeConstants.hiPriorityThreadOSPriority
            }

        try {
            val curPrio = DaemonHelper.getPriority (tid)
            info ("Current priority for thread " + identifier
                  + " is " + curPrio);
        } catch {
            case e : OSException =>
                warn ("Failed to get priority for the current thread "
                      + identifier + ": " + e.getMessage, e)
        }

        try {
            DaemonHelper.setPriority (tid, osPriority)
            info ("Priority for thread " + identifier
                  + " has been successfuly changed to "
                  + osPriority)
        } catch {
            case e : OSException =>
                warn ("Failed to set priority to " + osPriority
                      + " for thread " + identifier
                      + ": " + e.getMessage, e)
        }
    }
}
