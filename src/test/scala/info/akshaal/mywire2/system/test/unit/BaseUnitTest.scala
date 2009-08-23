/*
 * BaseTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system.test.unit

import java.lang.reflect.Method

import org.testng.Assert._
import org.testng.annotations.{AfterMethod,
                               BeforeMethod,
                               AfterSuite,
                               BeforeSuite}

import info.akshaal.jacore.system.logger.Logging
import test.common.BaseTest

/**
 * Basic initialization.
 */
private[unit] object UnitTestState extends Logging {
    var ready = false

    def init () = {
        synchronized {
            if (!ready) {
                doInit
                ready = true
            }
        }
    }

    private def doInit () = {
        UnitTestModule // Touching

        info ("Initializing")
    }
}

/**
 * Base class for all tests.
 */
class BaseUnitTest extends BaseTest {
    @AfterMethod
    def checkDaemonStatus () = {
        assertFalse (UnitTestModule.daemonStatus.isDying,
                     "The application must not be dying at this moment!")
    }

    @BeforeSuite
    def beforeSuite0 () = {
        UnitTestState.init
    }
}
