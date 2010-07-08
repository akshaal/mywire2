/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package test
package unit.domain

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.test.JacoreSpecWithJUnit

import domain.LogRecordLevel

class LogRecordTest extends JacoreSpecWithJUnit ("LogRecord specification") {
    "LogRecord" should {
        "have correct ids" in {
            LogRecordLevel.Debug.id  must_==  0
            LogRecordLevel.Info.id   must_==  1
            LogRecordLevel.Warn.id   must_==  2
            LogRecordLevel.Error.id  must_==  3
            LogRecordLevel.BusinessLogicInfo.id     must_==  4
            LogRecordLevel.BusinessLogicWarning.id  must_==  5
            LogRecordLevel.BusinessLogicProblem.id  must_==  6
        }
    }
}
