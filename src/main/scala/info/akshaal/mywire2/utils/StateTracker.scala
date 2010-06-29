/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils

import info.akshaal.jacore.utils.ClassUtils
import domain.{StateUpdated, StateSensed}

class StateTracker [T] (names : String*) (implicit tManifest : ClassManifest[T])
                  extends AbstractTracker[java.lang.Object, T] (names : _*)
{
    private val boxedTClass = ClassUtils.box (tManifest.erasure)

    /**
     * Updates current state registry by using value from stateUpdated object.
     * Returns true if value is changed.
     */
    def updateFrom (stateUpdated : StateUpdated) : Boolean = {
        val value = stateUpdated.value
        val valueObj = value.asInstanceOf [java.lang.Object]
        val valueClass = ClassUtils.box (valueObj.getClass ())

        if (boxedTClass.isAssignableFrom(valueClass)) {
            update (stateUpdated.name, Some(value.asInstanceOf[T]))
        } else {
            if (names contains stateUpdated.name) {
                errorLazy ("Tracked state object contains value of incompatible type: " + stateUpdated)
            }

            false
        }
    }

    /**
     * Updates current state registry by using value from stateUpdated object.
     * Returns true if value is changed.
     */
    def updateFrom (stateSensed : StateSensed) : Boolean = {
        stateSensed.value match {
            case Some(value) =>
                val valueObj = value.asInstanceOf [java.lang.Object]
                val valueClass = ClassUtils.box (valueObj.getClass ())

                if (boxedTClass.isAssignableFrom(valueClass)) {
                    update (stateSensed.name, Some(value.asInstanceOf[T]))
                } else {
                    if (names contains stateSensed.name) {
                        errorLazy ("Tracked state object contains value of incompatible type: " + stateSensed)
                    }

                    false
                }

            case None =>
                update (stateSensed.name, None)
        }
    }

    /**
     * Returns a constructor for definition of a problem related to the named state.
     */
    def problemIf (name : String) : StateProblemConstructor = {
        require (names contains name, name + " is not tracked by this object")

        new StateProblemConstructor (name)
    }

    /**
     * Constructor for problem definitions related to a state with provided name.
     */
    class StateProblemConstructor (name : String) {
        def equalsTo (values : T*) : Problem =
            new Problem {
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
