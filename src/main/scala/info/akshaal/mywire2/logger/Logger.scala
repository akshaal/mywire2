package info.akshaal.mywire2.logger

import org.slf4j.LoggerFactory
import org.slf4j.{Logger => SlfLogger}

final class Logger (slfLogger : SlfLogger) {
    // Simple log
 
    @inline
    def debug (str : String) = slfLogger.debug (str)

    @inline
    def info (str : String)  = slfLogger.info (str)

    @inline
    def warn (str : String)  = slfLogger.warn (str)

    @inline
    def error (str : String) = slfLogger.error (str)
    
    // Log with exception
    
    @inline
    def debug (str : String, e : Throwable) = slfLogger.debug (str, e)

    @inline
    def info (str : String, e : Throwable)  = slfLogger.info (str, e)

    @inline
    def warn (str : String, e : Throwable)  = slfLogger.warn (str, e)

    @inline
    def error (str : String, e : Throwable) = slfLogger.error (str, e)
    
    // Lazy log
    
    @inline
    def debugLazy (obj : => AnyRef) =
        if (slfLogger.isDebugEnabled) this.debug (obj.toString)
    
    @inline
    def infoLazy (obj : => AnyRef) =
        if (slfLogger.isInfoEnabled) this.info (obj.toString)
    
    @inline
    def warnLazy (obj : => AnyRef)  =
        if (slfLogger.isWarnEnabled) this.warn (obj.toString)

    @inline
    def errorLazy (obj : => AnyRef) =
        if (slfLogger.isErrorEnabled) this.error (obj.toString)
    
    // Lazy log with exception
    
    @inline
    def debugLazy (obj : AnyRef, e : Throwable) =
        if (slfLogger.isDebugEnabled) this.debug (obj.toString, e)
    
    @inline
    def infoLazy (obj : AnyRef, e : Throwable)  =
        if (slfLogger.isInfoEnabled) this.info (obj.toString, e)
    
    @inline
    def warnLazy (obj : AnyRef, e : Throwable)  =
        if (slfLogger.isWarnEnabled) this.warn (obj.toString, e)
    
    @inline
    def errorLazy (obj : AnyRef, e : Throwable) =
        if (slfLogger.isErrorEnabled) this.error (obj.toString, e)
}

final object Logger {
    def get (name : String): Logger =
         new Logger (LoggerFactory.getLogger (name))

    def get (any : AnyRef): Logger = get (loggerNameForClass (any.getClass.getName))

    def get: Logger =
         get (loggerNameForClass(new Throwable().getStackTrace()(1).getClassName))

    private def loggerNameForClass (className : String) =
	if (className.endsWith("$"))
	    className.substring(0, className.length - 1)
        else
            className
}
