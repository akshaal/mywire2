/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils
package tracker

import java.util.HashMap
import scala.collection.JavaConversions._

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.logger.Logging

/**
 * Track some values.
 *
 * @tparam T type of values tracked by this tracker internally,
 *           this must be Object type, not primitive value type, because
 *           we don't want to have NPEs when getting value from HashMap...
 *
 * @tparam B type of values tracked by this tracker. This could be a primitive
 *           value type.
 *
 * @param names names to track. must not be empty.
 */
abstract class AbstractTracker[T, B] (names : String*) extends Logging {
    protected final val createdAt = System.nanoTime.nanoseconds
    protected final val updatedAt = new HashMap [String, TimeValue]
    protected final val map = new HashMap [String, T]

    require (!names.isEmpty, "list of names must not be empty")

    /**
     * Returns current value or exception if unknown.
     *
     * @param name name of the tracked value to get
     * @return value corresponding to the given name
     */
    def apply (name : String) : B = {
        require (names contains name, name + " is not tracked by this tracker")

        val result : T = map.get (name)
        if (result == null) {
            throw new IllegalArgumentException ("No value available for name" +:+ name)
        }
        
        return result.asInstanceOf [B]
    }

    /**
     * Updates registry. Returns true if value is changed. If value with the given name
     * is not tracked, then this method returns false.
     *
     * @param name name of the value to update.
     * @param valueOption value to set for the given name
     * @return true if new value has replaced some other different value
     */
    def update (name : String, valueOption : Option[B]) : Boolean = {
        if (names contains name) {
            valueOption match {
                case None =>
                    map.remove (name) != null

                case Some(value) =>
                    updatedAt.put (name, System.nanoTime nanoseconds)
                    val previous = map.put (name, value.asInstanceOf[T])
                    previous == null || previous != value
            }
        } else {
            false
        }
    }

    /**
     * Returns a definition of a problem that appears when some value is unknown for too long.
     *
     * @param period period of time that must pass after the tracker creation, before
     *               the problem will be checked for first time
     * @return new problem definition
     */
    def problemIfUndefinedFor (period : TimeValue) : ProblemDetector =
        new ProblemDetector {
            /**
             * Check all names we track and find one that has no value for too long.
             * If [[perion]] has not passed yet or all values are defined, then returns None.
             *
             * @return name of undefined value or None if everything is fine for now
             */
            private def findUndefined : Option[String] = {
                if (System.nanoTime.nanoseconds - createdAt > period) {
                    for (name <- names) {
                        if (!(map contains name)) {
                            val lastUpdate = updatedAt.get (name)
                            if (lastUpdate == null || System.nanoTime.nanoseconds - lastUpdate > period) {
                                return Some (name)
                            }
                        }
                    }
                }

                return None
            }

            override def detected () : Option[String] = {
                for (name <- findUndefined)
                    yield "Value '" + name + "' unavailable for too long"
            }

            override def isGone () : Option[String] = {
                if (findUndefined.isDefined) None else Some ("All values are available now")
            }
        }

    /**
     * Returns a definition of problem that appears when some value is unknown for too long.
     * @return new problem definition
     */
    def problemIfUndefined () : ProblemDetector = problemIfUndefinedFor (0 nanoseconds)
}
