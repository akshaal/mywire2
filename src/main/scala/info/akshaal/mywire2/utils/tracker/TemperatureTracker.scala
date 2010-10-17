/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils
package tracker

import scala.collection.JavaConversions._

import info.akshaal.jacore.`package`._
import domain.Temperature

/**
 * Track temperatures.
 *
 * @param names temperature names to track
 */
class TemperatureTracker (names : String*)
                  extends AbstractTracker[java.lang.Double, Double] (names : _*)
{
    /**
     * Updates current temperature registry by using value from Temperature object.
     * Average3 is used as a value.
     *
     * @param temperature Temperature object to update value from
     * @return true if value is changed.
     */
    def updateFrom (temperature : Temperature) : Boolean =
        update (temperature.name, temperature.average3)

    /**
     * Returns a definition of problem that appears when some of values are NaN.
     *
     * @return a new definition of problem
     */
    def problemIfNaN : ProblemDetector =
        new ProblemDetector {
            /**
             * Find NaN among all tracked values.
             * 
             * @return name of the found NaN value or None if nothing found
             */
            private def findNaN : Option[String] = {
                for ((name, value) <- map) {
                    if (value.isNaN) {
                        return Some (name)
                    }
                }
                
                return None
            }

            override def detected () : Option[String] = {
                for (name <- findNaN)
                    yield "Temperature '" + name + "' unavailable"
            }

            override def isGone () : Option[String] = {
                if (findNaN.isDefined) None else Some ("All temperatures value are back")
            }
        }

    /**
     * Returns a constructor for definition of a problem related to the temperature.
     *
     * @param name of the tracked value
     * @return a constructo that can be used to create a problem detector for the given name
     */
    def problemIf (name : String) : TemperatureProblemDetectorConstructor = {
        require (names contains name, name + " is not tracked by this object")

        new TemperatureProblemDetectorConstructor (name)
    }

    /**
     * Constructor for problem definitions related to a temperature with provided name.
     *
     * @param name name of the tracked value
     */
    class TemperatureProblemDetectorConstructor (name : String) {
        /**
         * Returns detector to detect a case when value is greater then the given limit.
         *
         * @param limit highest allowed value
         * @param backOn in case if a problem is detected, then the problem will be
         *        considered gone iff value gets lower than [[backOn]] value.
         * @return new problem detector
         */
        def greaterThan (limit : Double, backOn : Double) : ProblemDetector =
            new ProblemDetector {
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

        /**
         * Returns detector to detect a case when value is lower then the given limit.
         *
         * @param limit lowest allowed value
         * @param backOn in case if a problem is detected, then the problem will be
         *        considered gone iff value gets higher than [[backOn]] value.
         * @return new problem detector
         */
        def lessThan (limit : Double, backOn : Double) : ProblemDetector =
            new ProblemDetector {
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
