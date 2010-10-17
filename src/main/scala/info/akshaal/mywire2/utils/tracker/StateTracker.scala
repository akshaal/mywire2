/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils
package tracker

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.utils.ClassUtils
import domain.{StateUpdated, StateSensed}

/**
 * State tracker. Tracks number of named states.
 *
 * @tparam type of state this tracker tracks.
 * @param names name of states to track
 */
class StateTracker [T] (names : String*) (implicit tManifest : ClassManifest[T])
                  extends AbstractTracker[java.lang.Object, T] (names : _*)
{
    private val boxedTClass = ClassUtils.box (tManifest.erasure)

    /**
     * Updates current state registry by using value from stateUpdated object.
     *
     * @param stateUpdated message to update from
     * @return true if value is updated (it means that the old value was different from new one)
     */
    def updateFrom (stateUpdated : StateUpdated) : Boolean = {
        val value = stateUpdated.value
        val valueObj = value.asInstanceOf [java.lang.Object]
        val valueClass = ClassUtils.box (valueObj.getClass ())

        if (boxedTClass.isAssignableFrom (valueClass)) {
            update (stateUpdated.name, Some (value.asInstanceOf[T]))
        } else {
            if (names contains stateUpdated.name) {
                errorLazy ("Tracked state object contains value of incompatible type" +:+ stateUpdated)
            }

            false
        }
    }

    /**
     * Updates current state registry by using value from stateUpdated object.
     *
     * @param stateSensed message to update from
     * @return true if value is updated (it means that the old value was different from new one)
     */
    def updateFrom (stateSensed : StateSensed) : Boolean = {
        stateSensed.value match {
            case Some (value) =>
                val valueObj = value.asInstanceOf [java.lang.Object]
                val valueClass = ClassUtils.box (valueObj.getClass ())

                if (boxedTClass.isAssignableFrom (valueClass)) {
                    update (stateSensed.name, Some(value.asInstanceOf[T]))
                } else {
                    if (names contains stateSensed.name) {
                        errorLazy ("Tracked state object contains value of incompatible type" +:+ stateSensed)
                    }

                    false
                }

            case None =>
                update (stateSensed.name, None)
        }
    }

    /**
     * Returns a constructor for definition of a problem detector related to the named state.
     *
     * @param name name of the state to check for problem
     * @return constructor to be used to create problem detectors associated
     *                      with the given state name
     */
    def problemIf (name : String) : StateProblemDetectorConstructor = {
        require (names contains name, name + " is not tracked by this object")

        new StateProblemDetectorConstructor (name)
    }

    /**
     * Constructor for problem definitions related to a state with provided name.
     *
     * @param name name of state to check for problems
     */
    class StateProblemDetectorConstructor (name : String) {
        /**
         * Creates a problem detector that considers given values as a manifistation of a problem.
         * @param values list of illegal (problematic) values
         * @return new problem detector
         */
        def equalsTo (values : T*) : ProblemDetector =
            new ProblemDetector {
                override def detected () : Option[String] = {
                    val valueObj = map.get (name)

                    if (valueObj == null || !(values contains valueObj)) {
                        None
                    } else {
                        Some ("State '" + name + "' " + valueObj + " is " + valueObj)
                    }
                }

                override def isGone () : Option[String] = {
                    val valueObj = map.get (name)

                    if (valueObj == null || (values contains valueObj)) {
                        None
                    } else {
                        Some ("State '" + name + "' " + valueObj + " is back to normal")
                    }
                }
            }
    }
}
