/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.test.unit.logger

import info.akshaal.mywire2.daemon.Daemon
import info.akshaal.mywire2.logger.Logger
import info.akshaal.mywire2.test.common.BaseTest

import org.testng.annotations.Test

class LoggerTest extends BaseTest {  
    @Test (groups=Array("indie"))
    def testLoggerLevels () = {
        logger.debug ("Debug level")
        logger.info ("Info level")
        logger.warn ("Warn level")
        logger.error ("Error level")
        
        logger.debugLazy (sideEffect ("debug lazy"))
        logger.infoLazy (sideEffect ("info lazy"))
        logger.warnLazy (sideEffect ("warn lazy"))
        logger.errorLazy (sideEffect ("error lazy"))        
    }
    
    private def sideEffect (str : String) = {
        logger.debug ("Computing argument for lazy log message: " + str)
        str
    }
}
