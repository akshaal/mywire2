/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.strategy

import info.akshaal.jacore.test.JacoreSpecWithJUnit
import info.akshaal.jacore.`package`._

import strategy.SimpleOnOffStrategy

class StrategiesTest extends JacoreSpecWithJUnit ("Strategies specification") {
    "SimpleOnOffStrategy" should {
        "work properly" in {
            val s = new SimpleOnOffStrategy (onInterval = 50 milliseconds,
                                             offInterval = 30 milliseconds)

            // Wait till it is on + 15ms
            val startStateUpdate = s.getStateUpdate
            if (startStateUpdate.state == false) {
                Thread.sleep (startStateUpdate.validTime.asMilliseconds + 15)
            } else {
                Thread.sleep (startStateUpdate.validTime.asMilliseconds + 15 + 30)
            }            

            // Do testing
            s.getStateUpdate.state  must_== true
            (!s.getStateUpdate).state  must_== false

            Thread.sleep (50)

            s.getStateUpdate.state  must_== false
            (!s.getStateUpdate).state  must_== true

            Thread.sleep (30)

            s.getStateUpdate.state  must_== true
            (!s.getStateUpdate).state  must_== false

            Thread.sleep (50)

            s.getStateUpdate.state  must_== false
            (!s.getStateUpdate).state  must_== true
        }
    }
}
