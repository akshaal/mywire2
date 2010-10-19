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
            val onTime = 200 milliseconds
            val offTime = 120 milliseconds
            val s = new SimpleOnOffStrategy (onInterval = onTime, offInterval = offTime)

            // Wait till it is on + 15ms
            val startStateUpdate = s.getStateUpdate
            if (startStateUpdate.state == false) {
                Thread.sleep (startStateUpdate.validTime.asMilliseconds + 15)
            } else {
                Thread.sleep ((startStateUpdate.validTime + offTime).asMilliseconds + 15)
            }            

            // Do testing
            s.getStateUpdate.state  must_== true
            (!s.getStateUpdate).state  must_== false

            Thread.sleep (onTime.asMilliseconds)

            s.getStateUpdate.state  must_== false
            (!s.getStateUpdate).state  must_== true

            Thread.sleep (offTime.asMilliseconds)

            s.getStateUpdate.state  must_== true
            (!s.getStateUpdate).state  must_== false

            Thread.sleep (onTime.asMilliseconds)

            s.getStateUpdate.state  must_== false
            (!s.getStateUpdate).state  must_== true
        }
    }
}
