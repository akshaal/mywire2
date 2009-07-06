package info.akshaal.mywire2.actor

import scala.collection.mutable.HashSet

private[actor] object Monitoring {
    def add (actor : MywireActor) = {
	val cmd = Add (actor)
        Monitor1 ! cmd
        Monitor2 ! cmd
    }

    def remove (actor : MywireActor) = {
	val cmd = Remove (actor)
        Monitor1 ! cmd
        Monitor2 ! cmd
    }
}

private[actor] abstract sealed class MonitoringCommand
private[actor] final case class Add (actor : MywireActor) extends MonitoringCommand
private[actor] final case class Remove (actor : MywireActor) extends MonitoringCommand
private[actor] final case class Ping extends MonitoringCommand
private[actor] final case class Pong extends MonitoringCommand
private[actor] final case class Monitor extends MonitoringCommand

private[actor] object MonitoringActor1 extends Monitor
private[actor] object MonitoringActor2 extends Monitor

private[actor] class MonitoringActor extends MywireActor {
    schedule payload Monitor() every RuntimeConstants.actorsMonitoringIntervalSeconds seconds

    private var currentActors = new HashSet[MywireActor]
    private var monitoringActors : Option[HashSet[MywireActor]] = None

    def act () = {
        case Add (actor)    => set += actor
        case Remove (actor) => set -= actor

        case TimeOut(Monitor) => monitor

        case Pong => monitoringActors -= sender
    }

    private def monitor () = {
	monitoringActors match {
	    case None => ()
	    case Some (actors) => check (actors)
	}

        monitoringActors = Some (currentActors clone)
        monitoringActors.foreach (_ ! Ping())
    }

    private def check (actors : HashSet[MywireActor]) = {
	val notResponding = actors.filter (!currentActors.contains (_))

        if (notResponding.isEmpty) {
	    // TODO: Implement
	}
    }
}
