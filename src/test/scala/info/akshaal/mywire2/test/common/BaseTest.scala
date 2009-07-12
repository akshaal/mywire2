/*
 * BaseTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.common

import java.lang.reflect.Method

import org.testng.annotations.{AfterMethod,
                               BeforeMethod,
                               AfterSuite,
                               BeforeSuite}

import info.akshaal.mywire2.RuntimeConstants
import info.akshaal.mywire2.logger.Logging

private[common] object TestState extends Logging {
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
        info ("Initializing")
    }
}

trait BaseTest extends Logging {
    @BeforeSuite
    def beforeSuite () {
        TestState.init
    }

    @AfterSuite
    def AfterSuite () {
    }

    @BeforeMethod
    def beforeEachMethod (method : Method) {
        val methodSignature = method.toGenericString
        
        info ("About to run test: " + methodSignature)
    }

    @AfterMethod
    def afterEachMethod (method : Method) {
        val methodSignature = method.toGenericString

        info ("Test has completed its execution: " + methodSignature)
    }


}
