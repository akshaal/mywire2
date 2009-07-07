/*
 * ActorScheduling.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

final class TimeUnit (nano : Long) {
    def asNanoseconds       = nano

    lazy val asMicroseconds = nano / 1000L
    lazy val asMilliseconds = nano / 1000L / 1000L
    lazy val asSeconds      = nano / 1000L / 1000L / 1000L
    lazy val asMinutes      = nano / 1000L / 1000L / 1000L / 60L
    lazy val asHours        = nano / 1000L / 1000L / 1000L / 60L / 60L
    lazy val asDays         = nano / 1000L / 1000L / 1000L / 60L / 60L / 24L
}