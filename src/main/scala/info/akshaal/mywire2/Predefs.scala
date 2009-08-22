/*
 * Implicits.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2

import java.io.{FileInputStream, IOException, Closeable}

import system.logger.Logger
import system.utils.TimeUnit

object Predefs {
    /**
     * Create list repeating code n times.
     */
    @inline
    def repeatToList[T] (n : Int) (code : => T) : List[T] = {
        var l : List[T] = Nil
        var i = n

        while (i > 0) {
            i -= 1
            l = code :: l
        }

        l
    }

    /**
     * Create object of interface Runnable which will execute the given
     * block of code.
     */
    @inline
    def mkRunnable (code : => Unit) = {
        new Runnable () {
            def run () {
                code
            }
        }
    }

    /**
     * Convert possible null value using code provided.
     * @param ref possibly null value
     * @param code run this code if ref is null
     *
     * TODO: This method must return NotNull instance
     */
    @inline
    def convertNull[T] (ref : T) (code : => T) : T = {
        if (ref == null) code else ref.asInstanceOf[T]
    }

    /**
     * Execute code with closeable IO.
     * TODO: Code must be executed with NotNull argument
     */
    @inline
    def withCloseableIO[I <: Closeable, T] (createCode : => I) (code : I => T) : T = {
        var inputStream : I = null.asInstanceOf[I]

        try {
            inputStream = createCode
            code (inputStream.asInstanceOf[I])
        } catch {
            case ex : IOException =>
                throw new IOException ("Error during access to input stream: "
                                       + ex.getMessage,
                                       ex)
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close ()
                } catch {
                    case ex : IOException =>
                        throw new IOException ("Error closing input stream: "
                                               + ": " + ex.getMessage,
                                               ex)
                }
            }
        }
    }

    /**
     * Execute block of code and print a message if block of code throws
     * an exception.
     */
    @inline
    def logIgnoredException (message : => String) (code : => Unit) (implicit logger : Logger) =
    {
        try {
            code
        } catch {
            case ex: Exception =>
                logger.error (message, ex)
        }
    }

    // /////////////////////////////////////////////////////////////////////
    // Time

    /**
     * Converts Long to TimeUnitFromNumberCreator
     */
    @inline
    implicit def long2TimeUnitFromNumberCreator (x : Long) =
        new TimeUnitFromNumberCreator (x)

    /**
     * Converts Int to TimeUnitFromLongCreator
     */
    @inline
    implicit def int2TimeUnitFromNumberCreator (x : Int) =
        new TimeUnitFromNumberCreator (x)

    @inline
    implicit def string2TimeUnit (x : String) = TimeUnit.parse (x)

    /**
     * Wrapper for Long that makes it possible to convert
     * it to TimeUnit object.
     */
    final class TimeUnitFromNumberCreator (x : Long) extends NotNull {
        @inline
        def nanoseconds  = mk (x)

        @inline
        def microseconds = mk (x * 1000L)

        @inline
        def milliseconds = mk (x * 1000L * 1000L)

        @inline
        def seconds      = mk (x * 1000L * 1000L * 1000L)

        @inline
        def minutes      = mk (x * 1000L * 1000L * 1000L * 60L)

        @inline
        def hours        = mk (x * 1000L * 1000L * 1000L * 60L * 60L)

        @inline
        def mk (nano : Long) = new TimeUnit (nano)
    }
}
