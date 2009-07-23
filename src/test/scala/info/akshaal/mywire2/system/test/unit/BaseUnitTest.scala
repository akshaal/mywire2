/*
 * BaseTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.test.unit

import java.lang.reflect.Method

import org.testng.annotations.{AfterMethod,
                               BeforeMethod,
                               AfterSuite,
                               BeforeSuite}

import mywire2.system.RuntimeConstants
import mywire2.system.logger.Logging
import mywire2.test.common.BaseTest

/**
 * Basic initialization.
 */
private[unit] object UnitTestState extends Logging {
    var ready = false

    def init () {
        synchronized {
            if (!ready) {
                doInit
                ready = true
            }
        }
    }

    private def doInit () {
        UnitTestModule // Touching

        info ("Initializing")
    }
}

/**
 * Base class for all tests.
 */
class BaseUnitTest extends BaseTest {
    @BeforeSuite
    override def beforeSuite () {
        UnitTestState.init
        
        super.beforeSuite
    }
}
