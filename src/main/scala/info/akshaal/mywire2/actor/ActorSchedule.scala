/*
 * ActorScheduling.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.actor

import info.akshaal.mywire2.scheduler.Scheduler

class TimeSpec[T] (number : Long, action : Long => T) {
    def nanoseconds = {
        action (number)
    }

    def microseconds = {
        action (number * 1000L)
    }

    def miliseconds = {
        action (number * 1000L * 1000L)
    }

    def seconds = {
        action (number * 1000L * 1000L * 1000L)
    }

    def minutes = {
        action (number * 1000L * 1000L * 1000L * 60L)
    }

    def hours = {
        action (number * 1000L * 1000L * 1000L * 60L * 60L)
    }

    def days = {
        action (number * 1000L * 1000L * 1000L * 60L * 60L * 24L)
    }
}

class Trigger (actor : MywireActor, payload : Any) {
    def in (number : Int) = {
        new TimeSpec[Unit] (number, scheduleIn)
    }

    def scheduleIn (nanos : Long) = {
        Scheduler.inNano (actor, payload, nanos)
    }

    def every (number : Int) = {
        new TimeSpec[Unit] (number, scheduleEvery)
    }

    def scheduleEvery (nanos : Long) = {
        Scheduler.everyNano (actor, payload, nanos)
    }
}

class ActorSchedule (actor : MywireActor) {
    def payload (payload : Any) = {
        new Trigger (actor, payload)
    }
}