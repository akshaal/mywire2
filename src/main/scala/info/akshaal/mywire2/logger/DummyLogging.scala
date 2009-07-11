/*
 * Logging.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.logger

trait DummyLogging extends Logging {
    protected[logger] override val logger = null

    override def debug (str : String) = ()
    override def info (str : String)  = println ("INFO: " + str)
    override def warn (str : String)  = System.err.println ("WARN: " + str)
    override def error (str : String) = System.err.println ("ERROR: " + str)

    // Log with exception

    override def debug (str : String, e : Throwable) = ()
    override def info (str : String, e : Throwable)  = {
        println (str)
        e.printStackTrace
    }

    override def warn (str : String, e : Throwable)  = {
        println ("WARN: " + str)
        e.printStackTrace
    }

    override def error (str : String, e : Throwable) = {
        println (str)
        e.printStackTrace
    }

    // Lazy log

    override def debugLazy (obj : => AnyRef) = ()

    override def infoLazy (obj : => AnyRef)  = info (obj.toString)
    override def warnLazy (obj : => AnyRef)  = info (obj.toString)
    override def errorLazy (obj : => AnyRef) = info (obj.toString)

    // Lazy log with exception
    
    override def debugLazy (obj : AnyRef, e : Throwable) = ()

    override def infoLazy (obj : AnyRef, e : Throwable)  =
        info (obj.toString, e)

    override def warnLazy (obj : AnyRef, e : Throwable)  =
        warn (obj.toString, e)

    override def errorLazy (obj : AnyRef, e : Throwable) =
        error (obj.toString, e)
}
