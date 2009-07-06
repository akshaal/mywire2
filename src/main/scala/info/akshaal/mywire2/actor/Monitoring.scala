package info.akshaal.mywire2.actor

import scala.collection.mutable.HashSet
import scala.collection.mutable.Set

import info.akshaal.mywire2.scheduler.TimeOut
import info.akshaal.mywire2.RuntimeConstants

private[actor] object Monitoring {
    def add (actor : MywireActor) = {
        val cmd = Add (actor)

        MonitoringActor1 ! cmd
        MonitoringActor2 ! cmd
    }

    def remove (actor : MywireActor) = {
        val cmd = Remove (actor)
        
        MonitoringActor1 ! cmd
        MonitoringActor2 ! cmd
    }
}

private[actor] abstract sealed class MonitoringCommand
private[actor] final case class Add (actor : MywireActor) extends MonitoringCommand
private[actor] final case class Remove (actor : MywireActor) extends MonitoringCommand
private[actor] final case object Ping extends MonitoringCommand
private[actor] final case object Pong extends MonitoringCommand
private[actor] final case object Monitor extends MonitoringCommand

private[actor] object MonitoringActor1 extends MonitoringActor {
    MonitoringActor2 ! (Add (this))
}

private[actor] object MonitoringActor2 extends MonitoringActor {
    MonitoringActor1 ! (Add (this))
}

private[actor] class MonitoringActor extends MywireActor with HiSpeedPool {
    schedule payload Monitor every RuntimeConstants.actorsMonitoringIntervalSeconds seconds

    startSkippingMonitoring

    private var currentActors : Set[MywireActor] = new HashSet[MywireActor]
    private var monitoringActors : Set[MywireActor] = new HashSet[MywireActor]

    def act () = {
        case Add (actor)    => currentActors += actor
        case Remove (actor) => currentActors -= actor

        case TimeOut(Monitor) => monitor

        case Pong => monitoringActors -= sender
    }

    private def monitor () = {
        // Check currently monitoring actors
        val notResponding =
                monitoringActors.filter (!currentActors.contains (_))

        if (notResponding.isEmpty) {
            debug ("Actors are OK")
        } else {
            // TODO: Actors are not OK
        }

        // Start monitoring current set of actors
        monitoringActors = currentActors.clone
        monitoringActors.foreach (_ ! Ping)
    }
}