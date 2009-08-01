/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system
package actor

import scala.collection.mutable.HashSet
import scala.collection.mutable.Set

import daemon.DaemonStatus
import scheduler.{TimeOut, Scheduler, UnfixedScheduling}
import utils.{TimeUnit, NormalPriorityPool}

private[system] final class Monitoring
                    (monitoringActors : List[MonitoringActor])
{
    private[actor] final def add (actor : Actor) = {
        val cmd = Add (actor)

        monitoringActors.foreach (_ ! cmd)
    }

    private[actor] final def remove (actor : Actor) = {
        val cmd = Remove (actor)
        
        monitoringActors.foreach (_ ! cmd)
    }
}

private[actor] abstract sealed class MonitoringCommand extends NotNull
private[actor] final case class Add (actor : Actor) extends MonitoringCommand
private[actor] final case class Remove (actor : Actor) extends MonitoringCommand
private[actor] case object Ping extends MonitoringCommand
private[actor] case object Pong extends MonitoringCommand
private[actor] case object Monitor extends MonitoringCommand

private[system] class MonitoringActor
                            (pool : NormalPriorityPool,
                             scheduler : Scheduler,
                             interval : TimeUnit,
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
