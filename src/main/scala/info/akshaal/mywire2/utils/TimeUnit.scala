/*
 * ActorScheduling.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

final class TimeUnit (nano : Long) {
    def asNanoseconds  = nano
    def asMicroseconds = nano / 1000L
    def asMilliseconds = nano / 1000L / 1000L
    def asSeconds      = nano / 1000L / 1000L / 1000L
    def asMinutes      = nano / 1000L / 1000L / 1000L / 60L
    def asHours        = nano / 1000L / 1000L / 1000L / 60L / 60L
    def asDays         = nano / 1000L / 1000L / 1000L / 60L / 60L / 24L
}