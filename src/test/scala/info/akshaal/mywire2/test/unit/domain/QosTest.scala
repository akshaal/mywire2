/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.domain

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import domain.Qos

class QosTest extends JacoreSpecWithJUnit ("QoS object specification") {
    "Qos" should {
        "be exported correctly" in {
            val t = new Qos (memoryUsedPercent = 30.0,
                             schedulerLatencyNs = 1,
                             hiPoolExecutionNs = 2,
                             hiPoolLatencyNs = 3,
                             normalPoolExecutionNs = 4,
                             normalPoolLatencyNs = 5,
                             lowPoolExecutionNs = 6,
                             lowPoolLatencyNs = 7)
            val tm = t.toMap
            tm ("memoryUsedPercent")  must_==  30.0
            tm ("schedulerLatencyNs")  must_==  1
            tm ("hiPoolExecutionNs")  must_==  2
            tm ("hiPoolLatencyNs")  must_==  3
            tm ("normalPoolExecutionNs")  must_==  4
            tm ("normalPoolLatencyNs")  must_==  5
            tm ("lowPoolExecutionNs")  must_==  6
            tm ("lowPoolLatencyNs")  must_==  7
        }
    }
}
