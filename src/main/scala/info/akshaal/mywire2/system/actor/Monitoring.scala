/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.actor

import scala.collection.mutable.HashSet
import scala.collection.mutable.Set

import daemon.Daemon
import scheduler.{TimeOut, Scheduler}
import system.RuntimeConstants
import utils.{TimeUnit, NormalPriorityPool}

private[system] abstract class Monitoring
{
    val monitoringActors : List[MonitoringActor]

    // -- - - - - -  - -- -- - -  - - - - - - --  - -- - - -
    // Concrete

    final def add (actor : Actor) = {
        val cmd = Add (actor)

        monitoringActors.foreach (_ ! cmd)
    }

    final def remove (actor : Actor) = {
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

private[system] abstract class MonitoringActor extends Actor
{
    schedule payload Monitor every interval

    protected val pool : NormalPriorityPool

    protected val interval : TimeUnit

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Concrete

    private[this] val currentActors : Set[Actor] = new HashSet[Actor]
    private[this] var monitoringActors : Set[Actor] = new HashSet[Actor]

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
        } else {
            error ("There are actors not responding: " + notResponding)
            Daemon.die ()
        }

        // Start monitoring current set of actors
        monitoringActors = currentActors.clone
        monitoringActors.foreach (_ ! Ping)
    }
}
