/*
 * ActorScheduling.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system.utils

import Predefs._

final class TimeUnit (nano : Long) extends NotNull
{
    @inline
    def asNanoseconds       = nano

    lazy val asMicroseconds = nano / TimeUnit.nsInUs
    lazy val asMilliseconds = nano / TimeUnit.nsInMs
    lazy val asSeconds      = nano / TimeUnit.nsInSec
    lazy val asMinutes      = nano / TimeUnit.nsInMin
    lazy val asHours        = nano / TimeUnit.nsInHour
    lazy val asDays         = nano / TimeUnit.nsInDay

    override lazy val toString = {
        // Split into components
        var cur = nano
        var comps : List[String] = Nil

        for ((name : String, value : Long) <- TimeUnit.units) {
            val div = cur / value
            cur = cur % value

            if (div != 0L) {
                comps = (div + name) :: comps
            }
        }

        // Return
        if (comps == Nil) "0ns" else comps.reverse.mkString(" ")
    }

    def + (that : TimeUnit) = new TimeUnit (nano + that.asNanoseconds)
    def - (that : TimeUnit) = new TimeUnit (nano - that.asNanoseconds)
    def * (that : TimeUnit) = new TimeUnit (nano * that.asNanoseconds)
    def * (that : Int) = new TimeUnit (nano * that.asInstanceOf[Long])
    def / (that : TimeUnit) = new TimeUnit (nano / that.asNanoseconds)
    def / (that : Int) = new TimeUnit (nano / that.asInstanceOf[Long])

    override def equals (that : Any) = that match {
        case thatTimeUnit : TimeUnit => nano == thatTimeUnit.asNanoseconds
    }

    override def hashCode : Int = nano.asInstanceOf[Int]

    def compare (that: TimeUnit) : Int =
        this.asNanoseconds compare that.asNanoseconds

    def equals (that: TimeUnit) : Boolean = compare(that) == 0
    def <= (that: TimeUnit)     : Boolean = compare(that) <= 0
    def >= (that: TimeUnit)     : Boolean = compare(that) >= 0
    def <  (that: TimeUnit)     : Boolean = compare(that) < 0
    def >  (that: TimeUnit)     : Boolean = compare(that) > 0
}

private[mywire2] object TimeUnit {
    private[utils] val nsInUs   = 1000L
    private[utils] val nsInMs   = 1000000L
    private[utils] val nsInSec  = 1000000000L
    private[utils] val nsInMin  = 60000000000L
    private[utils] val nsInHour = 3600000000000L
    private[utils] val nsInDay  = 86400000000000L

    private[utils] val units : List[(String, Long)] =
            List (("days", nsInDay),
                  ("hours", nsInHour),
                  ("mins", nsInMin),
                  ("secs", nsInSec),
                  ("ms", nsInMs),
                  ("us", nsInUs),
                  ("ns", 1L))

    /**
     * Parse string ot time unit.
     */
    def parse (str : String) : TimeUnit = TimeUnitParser.parse (str)

    import scala.util.parsing.combinator._

    /**
     * Parser
     */
    private object TimeUnitParser extends JavaTokenParsers {
        def parse (str : String) : TimeUnit =
            parseAll (expr, str) match {
               case Success (l, _) => l

               case Failure (m, _) =>
                   throw new IllegalArgumentException (
                              "Failed to parse time: " + str + ": " + m)

               case Error (m, _)   => throw new IllegalArgumentException (m)
                   throw new IllegalArgumentException (
                              "Error while parsing time: " + str + ": " + m)
            }

        def expr : Parser[TimeUnit] =
            timeUnit ~ rep(timeUnit) ^^ {
                case u1 ~ l => l.foldLeft (u1) {_ + _}
            }

        def timeUnit : Parser[TimeUnit] = (
              decimalNumber <~ "seconds"      ^^ (_.toLong.seconds)
            | decimalNumber <~ "milliseconds" ^^ (_.toLong.milliseconds)
            | decimalNumber <~ "nanoseconds"  ^^ (_.toLong.nanoseconds)
            | decimalNumber <~ "microseconds" ^^ (_.toLong.microseconds)
            | decimalNumber <~ "hours"        ^^ (_.toLong.hours)
            | decimalNumber <~ "minutes"      ^^ (_.toLong.minutes)
        )
    }
}