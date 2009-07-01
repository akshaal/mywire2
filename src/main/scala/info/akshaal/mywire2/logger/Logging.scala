/*
 * Logging.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.logger

trait Logging {
    private[logger] val logger = Logger.get (this)

    def debug (str : String) = logger.debug (str)
    def info (str : String)  = logger.info (str)
    def warn (str : String)  = logger.warn (str)
    def error (str : String) = logger.error (str)

    // Log with exception

    def debug (str : String, e : Throwable) = logger.debug (str, e)
    def info (str : String, e : Throwable)  = logger.info (str, e)
    def warn (str : String, e : Throwable)  = logger.warn (str, e)
    def error (str : String, e : Throwable) = logger.error (str, e)

    // Lazy log

    def debugLazy (obj : => AnyRef) = logger.debugLazy (obj)
    def infoLazy (obj : => AnyRef)  = logger.infoLazy (obj)
    def warnLazy (obj : => AnyRef)  = logger.warnLazy (obj)
    def errorLazy (obj : => AnyRef) = logger.errorLazy (obj)

    // Lazy log with exception
    
    def debugLazy (obj : AnyRef, e : Throwable) = logger.debugLazy (obj, e)
    def infoLazy (obj : AnyRef, e : Throwable)  = logger.infoLazy (obj, e)
    def warnLazy (obj : AnyRef, e : Throwable)  = logger.warnLazy (obj, e)
    def errorLazy (obj : AnyRef, e : Throwable) = logger.errorLazy (obj, e)
}
