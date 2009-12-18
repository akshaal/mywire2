/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package daemon

import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named

import info.akshaal.jacore.utils.{TimeUnit, LowPriorityPool, HiPriorityPool,
                                  NormalPriorityPool}
import info.akshaal.jacore.actor.{NormalPriorityActorEnv, Actor}
import info.akshaal.jacore.scheduler.Scheduler
import info.akshaal.jacore.daemon.DaemonStatus

import domain.Qos

/**
 * Gathers QOS parameters and broadcasts them to all interested.
 */
@Singleton
private[mywire2] class QosActor @Inject() (
                             normalPriorityActorEnv : NormalPriorityActorEnv,
                             @Named("mywire.qos.interval") interval : TimeUnit,
                             hiPriorityPool : HiPriorityPool,
                             lowPriorityPool : LowPriorityPool,
                             normalPriorityPool : NormalPriorityPool,
                             daemonStatus : DaemonStatus,
                             scheduler : Scheduler)
                         extends Actor (actorEnv = normalPriorityActorEnv)
{
    schedule every interval executionOf {
        if (daemonStatus.isQosAllowed) {
            // Calculate memory used
            val runtime = Runtime.getRuntime ()
            val max = runtime.maxMemory ()
            val total = runtime.totalMemory ()
            val free = runtime.freeMemory ()
            val used = total - free
            val usedPerc : Double = 100.0 * used / max

            // Create QOS object
            val qos =
                new Qos (
                    memoryUsedPercent = usedPerc,
                    schedulerLatencyNs = scheduler.averageLatency.asNanoseconds,
                    hiPoolExecutionNs = hiPriorityPool.executionTiming.average.asNanoseconds,
                    hiPoolLatencyNs = hiPriorityPool.latencyTiming.average.asNanoseconds,
                    normalPoolExecutionNs = normalPriorityPool.executionTiming.average.asNanoseconds,
                    normalPoolLatencyNs = normalPriorityPool.latencyTiming.average.asNanoseconds,
                    lowPoolExecutionNs = lowPriorityPool.executionTiming.average.asNanoseconds,
                    lowPoolLatencyNs = lowPriorityPool.latencyTiming.average.asNanoseconds)

            // Broadcast
            broadcaster.broadcast (qos)
        }
    }
}
