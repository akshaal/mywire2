/*
 * ActorScheduling.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.actor

import info.akshaal.mywire2.scheduler.Scheduler
import info.akshaal.mywire2.utils.TimeUnit
import info.akshaal.mywire2.Predefs._

final class TimeSpec[T] (number : Long, action : TimeUnit => T) extends NotNull {
    def nanoseconds  = action (number.nanoseconds)
    def microseconds = action (number.microseconds)
    def milliseconds = action (number.milliseconds)
    def seconds      = action (number.seconds)
    def minutes      = action (number.minutes)
    def hours        = action (number.hours)
    def days         = action (number.days)
}

final class Trigger (actor : Actor, payload : Any) extends NotNull {
    def in (number : Long)    = new TimeSpec[Unit] (number, scheduleIn)
    def every (number : Long) = new TimeSpec[Unit] (number, scheduleEvery)

    def in (time : TimeUnit)    = scheduleIn (time)
    def every (time : TimeUnit) = scheduleEvery (time)

    private def scheduleIn (time : TimeUnit)    = Scheduler.in (actor, payload, time)
    private def scheduleEvery (time : TimeUnit) = Scheduler.every (actor, payload, time)
}

final class ActorSchedule (actor : Actor) extends NotNull {
    def payload (payload : Any) = new Trigger (actor, payload)
}