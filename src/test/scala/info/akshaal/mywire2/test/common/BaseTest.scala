/*
 * BaseTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.test.common

import java.lang.reflect.Method

import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod

import info.akshaal.mywire2.logger.Logging

trait BaseTest extends Logging {
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
