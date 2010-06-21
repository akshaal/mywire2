/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils

import scala.collection.JavaConversions._

import info.akshaal.jacore.`package`._
import domain.Temperature

/**
 * Track temperatures.
 */
class TemperatureTracker (names : String*) extends AbstractTracker[java.lang.Double, Double] (names : _*) {
    /**
     * Returned when value is unavailable.
     */
    protected override def undefined_value : Double = Double.NaN

    /**
     * Updates current temperature registry by using value from Temperature object.
     * Average3 is used as a value.
     * Returns true if value is changed.
     */
    def updateFrom (temperature : Temperature) : Boolean =
        update (temperature.name, temperature.average3)

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
                    val valueObj = map.get (name)
                    val value = valueObj.asInstanceOf[Double]
                    if (valueObj == null || value.isNaN || value <= limit) {
                        None
                    } else {
                        Some ("Temperature '" + name + "' " + value + " is greater than " + limit)
                    }
                }

                override def isGone () : Option[String] = {
                    val valueObj = map.get (name)
                    val value = valueObj.asInstanceOf[Double]
                    if (valueObj == null || value.isNaN || value > backOn) {
                        None
                    } else {
                        Some ("Temperature '" + name + "' " + value + " is back to normal")
                    }
                }
            }

        def lessThan (limit : Double, backOn : Double) : Problem =
            new Problem {
                override def detected () : Option[String] = {
                    val valueObj = map.get (name)
                    val value = valueObj.asInstanceOf[Double]
                    if (valueObj == null || value.isNaN || value >= limit) {
                        None
                    } else {
                        Some ("Temperature '" + name + "' " + value + " is less than " + limit)
                    }
                }

                override def isGone () : Option[String] = {
                    val valueObj = map.get (name)
                    val value = valueObj.asInstanceOf[Double]
                    if (valueObj == null || value.isNaN || value < backOn) {
                        None
                    } else {
                        Some ("Temperature '" + name + "' " + value + " is back to normal")
                    }
                }
            }
    }
}
