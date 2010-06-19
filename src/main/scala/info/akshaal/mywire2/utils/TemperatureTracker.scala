/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils

import java.util.HashMap
import scala.collection.JavaConversions._

import info.akshaal.jacore.`package`._
import domain.Temperature

/**
 * Track temperatures.
 */
class TemperatureTracker (names : String*) {
    private final val createdAt = System.nanoTime.nanoseconds
    private final val map = new HashMap [String, Double]

    require (!names.isEmpty, "list of names must not be empty")

    /**
     * Returns current temperature value or null if unknown.
     */
    def apply (name : String) : Double = {
        require (names contains name, name + " is not tracked by this object")

        map.get (name)
    }

    /**
     * Updates current temperature registry by using value from Temperature object.
     * Average3 is used as a value.
     * Returns true if value is changed.
     */
    def updateFrom (temperature : Temperature) : Boolean = {
        if (names contains temperature.name) {
            val previous = map.put (temperature.name, temperature.average3)

            return previous == null || previous != temperature.average3
        } else {
            return false
        }
    }

    /**
     * Returns a definition of problem that appears when some of values are NaN.
     */
    def problemIfNaN : Problem =
        new Problem {
            private def getNaN : Option[String] = {
                for ((name, value) <- map) {
                    if (value.isNaN) {
                        return Some (name)
                    }
                }
                
                return None
            }

            override def detected () : Option[String] = {
                for (name <- getNaN) {
                    return Some ("Temperature '" + name + "' unavailable")
                }

                return None
            }

            override def isGone () : Option[String] = {
                for (name <- getNaN) {
                    return None
                }

                return Some ("All temperatures value are back")
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
                    return Some ("Temperature '" + name + "' unavailable for too long")
                }

                return None
            }

            override def isGone () : Option[String] = {
                for (name <- getUnknown) {
                    return None
                }

                return Some ("All temperatures value are read")
            }
        }

    /**
     * Returns a constructor for definition of a problem related to the temperature.
     */
    def problemIf (name : String) : TemperatureProblemConstructor = {
        require (names contains name, name + " is not tracked by this object")

        new TemperatureProblemConstructor (name)
    }

    /**
     * Constructor for problem definitions related to a temperature with provided name.
     */
    class TemperatureProblemConstructor (name : String) {
        def greaterThan (limit : Double, backOn : Double) : Problem =
            new Problem {
                override def detected () : Option[String] = {
                    val value = map.get (name)
                    if (value == null || value.isNaN || value <= limit) {
                        None
                    } else {
                        Some ("Temperature '" + name + "' " + value + " is greater than " + limit)
                    }
                }

                override def isGone () : Option[String] = {
                    val value = map.get (name)
                    if (value == null || value.isNaN || value > backOn) {
                        None
                    } else {
                        Some ("Temperature '" + name + "' " + value + " is back to normal")
                    }
                }
            }

        def lessThan (limit : Double, backOn : Double) : Problem =
            new Problem {
                override def detected () : Option[String] = {
                    val value = map.get (name)
                    if (value == null || value.isNaN || value >= limit) {
                        None
                    } else {
                        Some ("Temperature '" + name + "' " + value + " is less than " + limit)
                    }
                }

                override def isGone () : Option[String] = {
                    val value = map.get (name)
                    if (value == null || value.isNaN || value < backOn) {
                        None
                    } else {
                        Some ("Temperature '" + name + "' " + value + " is back to normal")
                    }
                }
            }
    }
}
