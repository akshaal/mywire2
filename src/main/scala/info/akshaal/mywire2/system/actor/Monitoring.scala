/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system
package actor

import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named

import scala.collection.mutable.HashSet
import scala.collection.mutable.Set

import daemon.DaemonStatus
import scheduler.{TimeOut, Scheduler, UnfixedScheduling}
import utils.{TimeUnit, NormalPriorityPool}

@Singleton
private[system] final class Monitoring @Inject()
                    (monitoringActors : MonitoringActors)
{
    private[actor] final def add (actor : Actor) = {
        val cmd = Add (actor)

        monitoringActors.monitoringActor1 ! cmd
        monitoringActors.monitoringActor2 ! cmd
    }

    private[actor] final def remove (actor : Actor) = {
        val cmd = Remove (actor)

        monitoringActors.monitoringActor1 ! cmd
        monitoringActors.monitoringActor2 ! cmd
    }
}

@Singleton
private[system] final class MonitoringActors @Inject() (
                        val monitoringActor1 : MonitoringActor,
                        val monitoringActor2 : MonitoringActor)

private[actor] abstract sealed class MonitoringCommand extends NotNull
private[actor] final case class Add (actor : Actor) extends MonitoringCommand
private[actor] final case class Remove (actor : Actor) extends MonitoringCommand
private[actor] case object Ping extends MonitoringCommand
private[actor] case object Pong extends MonitoringCommand
private[actor] case object Monitor extends MonitoringCommand

private[system] final class MonitoringActor @Inject() (
                     pool : NormalPriorityPool,
                     scheduler : Scheduler,
                     @Named("jacore.monitoring.interval") interval : TimeUnit,
                     daemonStatus : DaemonStatus)
            extends Actor (pool = pool,
                           scheduler = scheduler)
            with UnfixedScheduling
{
    schedule payload Monitor every interval

    private val currentActors : Set[Actor] = new HashSet[Actor]
    private var monitoringActors : Set[Actor] = new HashSet[Actor]

    final def act () = {
        case Add (actor)    => currentActors += actor
        case Remove (actor) => currentActors -= actor

        case TimeOut(Monitor) => monitor

        case Pong => sender.foreach (actor => monitoringActors -= actor)
    }

    private[this] def monitor () = {
        // Check currently monitoring actors
        val notResponding =
                monitoringActors.filter (currentActors.contains (_))

        if (notResponding.isEmpty) {
            debug ("Actors are OK")
            daemonStatus.monitoringAlive
        } else {
            error ("There are actors not responding: " + notResponding)
            daemonStatus.die
        }

        // Start monitoring current set of actors
        monitoringActors = currentActors.clone
        monitoringActors.foreach (_ ! Ping)
    }
}
