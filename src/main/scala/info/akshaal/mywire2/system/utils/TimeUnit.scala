/*
 * ActorScheduling.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.utils

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

private[utils] object TimeUnit {
    val nsInUs   = 1000L
    val nsInMs   = 1000000L
    val nsInSec  = 1000000000L
    val nsInMin  = 60000000000L
    val nsInHour = 3600000000000L
    val nsInDay  = 86400000000000L

    val units : List[(String, Long)] =
            List (("days", nsInDay),
                  ("hours", nsInHour),
                  ("mins", nsInMin),
                  ("secs", nsInSec),
                  ("ms", nsInMs),
                  ("us", nsInUs),
                  ("ns", 1L))
}