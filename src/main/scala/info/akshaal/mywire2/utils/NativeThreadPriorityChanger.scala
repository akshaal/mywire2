/*
 * newObject.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils

import info.akshaal.daemonhelper.{DaemonHelper, OSException}

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.logger.Logging
import info.akshaal.jacore.utils.ThreadPriorityChanger

@Singleton
private[mywire2] final class NativeThreadPriorityChanger @Inject() (
                            @Named ("mywire.os.priority.low") lowOsPriority : Int,
                            @Named ("mywire.os.priority.normal") normalOsPriority : Int,
                            @Named ("mywire.os.priority.hi") hiOsPriority : Int)
                       extends ThreadPriorityChanger with Logging
{
    import ThreadPriorityChanger._

    def toOsPriority (priority : Priority) : Int = priority match {
        case LowPriority    => lowOsPriority
        case NormalPriority => normalOsPriority
        case HiPriority     => hiOsPriority
    }

    def change (priority : Priority) : Unit = {
        val pid = DaemonHelper.getPid
        val tid = DaemonHelper.getTid
        val name = Thread.currentThread.getName
        val identifier = name + " (pid=" + pid + ", tid=" + tid + ")"
        val osPriority = toOsPriority (priority)

        try {
            val curPrio = DaemonHelper.getPriority (tid)
            debug ("Current priority for thread " + identifier
                   + " is " + curPrio);
        } catch {
            case e : OSException =>
                warn ("Failed to get priority for the current thread "
                      + identifier +:+ e, e)
        }

        try {
            DaemonHelper.setPriority (tid, osPriority)
            debug ("Priority for thread " + identifier
                   + " has been successfuly changed to "
                   + osPriority)
        } catch {
            case e : OSException =>
                warn ("Failed to set priority to " + osPriority
                      + " for thread " + identifier
                      +:+ e, e)
        }
    }
}
