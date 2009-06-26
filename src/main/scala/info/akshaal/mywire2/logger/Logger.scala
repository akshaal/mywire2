package info.akshaal.mywire2.logger

import org.slf4j.LoggerFactory
import org.slf4j.{Logger => SlfLogger}

// TODO: Logger must use actors to actually log

class Logger (slfLogger : SlfLogger) {
    // Simple log
  
    def debug (str : String) = slfLogger.debug (str)
    def info (str : String)  = slfLogger.info (str)
    def warn (str : String)  = slfLogger.warn (str)
    def error (str : String) = slfLogger.error (str)
    
    // Log with exception
    
    def debug (str : String, e : Throwable) = slfLogger.debug (str, e)
    def info (str : String, e : Throwable)  = slfLogger.info (str, e)
    def warn (str : String, e : Throwable)  = slfLogger.warn (str, e)
    def error (str : String, e : Throwable) = slfLogger.error (str, e)
    
    // Lazy log
    
    def debugLazy (obj : => AnyRef) =
        if (slfLogger.isDebugEnabled) this.debug (obj.toString)
    
    def infoLazy (obj : => AnyRef) =
        if (slfLogger.isInfoEnabled) this.info (obj.toString)
    
    def warnLazy (obj : => AnyRef)  =
        if (slfLogger.isWarnEnabled) this.warn (obj.toString)
            
    def errorLazy (obj : => AnyRef) =
        if (slfLogger.isErrorEnabled) this.error (obj.toString)
    
    // Lazy log with exception
    
    def debugLazy (obj : AnyRef, e : Throwable) =
        if (slfLogger.isDebugEnabled) this.debug (obj.toString, e)
    
    def infoLazy (obj : AnyRef, e : Throwable)  =
        if (slfLogger.isInfoEnabled) this.info (obj.toString, e)
    
    def warnLazy (obj : AnyRef, e : Throwable)  =
        if (slfLogger.isWarnEnabled) this.warn (obj.toString, e)
    
    def errorLazy (obj : AnyRef, e : Throwable) =
        if (slfLogger.isErrorEnabled) this.error (obj.toString, e)
}

object Logger {
    def get: Logger = {  
        val className =
          new Throwable().getStackTrace()(1).getClassName
          
          get (if (className.endsWith("$"))
                     className.substring(0, className.length - 1)
                 else
                     className)    
    }

    def get (name : String): Logger =
         new Logger (LoggerFactory.getLogger (name))
}
