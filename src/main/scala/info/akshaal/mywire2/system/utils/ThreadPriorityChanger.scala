/*
 * newObject.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system
package utils

import com.google.inject.{Inject, Singleton}

import Predefs._
import logger.Logging

import ru.toril.daemonhelper.{DaemonHelper, OSException}

@Singleton
private[system] final class ThreadPriorityChanger @Inject()
                                (prefs : Prefs)
                       extends Logging
{
    import ThreadPriorityChanger._

    def toOsPriority (priority : Priority) = priority match {
        case LowPriority    => prefs.getInt ("jacore.os.priority.low")
        case NormalPriority => prefs.getInt ("jacore.os.priority.normal")
        case HiPriority     => prefs.getInt ("jacore.os.priority.high")
    }

    def change (priority : Priority) = {
        val pid = DaemonHelper.getPid
        val tid = DaemonHelper.getTid
        val name = Thread.currentThread.getName
        val identifier = name + " (pid=" + pid + ", tid=" + tid + ")"
        val osPriority = toOsPriority (priority)

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

private[system] object ThreadPriorityChanger {
    abstract sealed class Priority
    case object LowPriority extends Priority
    case object NormalPriority extends Priority
    case object HiPriority extends Priority
}