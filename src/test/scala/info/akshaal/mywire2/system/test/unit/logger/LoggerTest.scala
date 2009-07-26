/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.test.unit.logger

import mywire2.system.daemon.Daemon
import mywire2.system.logger.Logger
import mywire2.system.test.unit.BaseUnitTest

import org.testng.annotations.Test

// TODO: Test appender

class LoggerTest extends BaseUnitTest {
    @Test (groups=Array("unit"))
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
