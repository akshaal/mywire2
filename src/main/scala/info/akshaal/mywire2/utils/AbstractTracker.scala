/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils

import java.util.HashMap
import scala.collection.JavaConversions._

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.logger.Logging

/**
 * Track some values.
 */
abstract class AbstractTracker[T, B] (names : String*) extends Logging {
    protected final val createdAt = System.nanoTime.nanoseconds
    protected final val map = new HashMap [String, T]

    require (!names.isEmpty, "list of names must not be empty")

    /**
     * Returns current value or NaN if unknown.
     */
    def apply (name : String) : B = {
        require (names contains name, name + " is not tracked by this tracker")

        val result = map.get (name)
        if (result == null) {
            throw new IllegalArgumentException ("No value available for name: " + name)
        } else {
            return result.asInstanceOf [B]
        }
    }

    /**
     * Updates registry. Returns true if value is changed.
     */
    def update (name : String, value : B) : Boolean = {
        if (names contains name) {
            val previous = map.put (name, value.asInstanceOf[T])

            return previous == null || previous != value
        } else {
            return false
        }
    }

    /**
     * Returns a definition of problem that appears when some temperature is unknown for too long.
     */
    def problemIfUndefinedFor (period : TimeValue) : Problem =
        new Problem {
            private def getUnknown : Option[String] = {
                if (System.nanoTime.nanoseconds - createdAt > period) {
                    for (name <- names) {
                        if (!(map contains name)) {
                            return Some (name)
                        }
                    }
                }

                return None
            }

            override def detected () : Option[String] = {
                for (name <- getUnknown) {
                    return Some ("Value '" + name + "' unavailable for too long")
                }

                return None
            }

            override def isGone () : Option[String] = {
                for (name <- getUnknown) {
                    return None
                }

                return Some ("All values are available now")
            }
        }
}
