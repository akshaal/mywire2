/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test.unit.strategy

import org.specs.SpecificationWithJUnit

import info.akshaal.jacore.`package`._

import strategy.SimpleOnOffStrategy

class StrategiesTest extends SpecificationWithJUnit ("Strategies specification") {
    "SimpleOnOffStrategy" should {
        "work properly" in {
            val s = new SimpleOnOffStrategy (onInterval = 50 milliseconds,
                                             offInterval = 30 milliseconds)

            // Wait till it is on + 15ms
            Thread.sleep (80 - System.currentTimeMillis % 80 + 15)

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
