/*
 * Implicits.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2

import java.io.{FileInputStream, IOException, Closeable}

import info.akshaal.mywire2.logger.Logger
import info.akshaal.mywire2.utils.TimeUnit

object Predefs {
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

    @inline
    def convertNull[T] (ref : T) (code : => T) : T = {
        if (ref == null) code else ref
    }

    /**
     * Execute code with closeable IO.
     */
    @inline
    def withCloseableIO[I <: Closeable, T] (createCode : => I) (code : I => T) : T = {
        var inputStream : I = null.asInstanceOf[I]

        try {
            inputStream = createCode
            code (inputStream)
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
     * Execute code with file input stream.
     */
    @inline
    def withFileInputStream[T] (filename : String) (code : FileInputStream => T) : T = {
        var inputStream : FileInputStream = null

        try {
            inputStream = new FileInputStream (filename)
            code (inputStream)
        } catch {
            case ex : IOException =>
                throw new IOException ("Error during access to file: "
                                       + filename + ": " + ex.getMessage,
                                       ex)
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close ()
                } catch {
                    case ex : IOException =>
                        throw new IOException ("Error closing file: "
                                               + filename
                                               + ": " + ex.getMessage,
                                               ex)
                }
            }
        }
    }

    /**
     * Execute block of code and print a message if block of code throws
     * an exception. If logger is not null, then logger is used to print
     * the message, otherwise message is shown through stderr stream.
     */
    @inline
    def logIgnoredException (logger : Logger,
                             message : => String) (code : => Unit) =
    {
        try {
            code
        } catch {
            case ex: Exception =>
                if (logger == null) {
                    System.err.println (message)
                    ex.printStackTrace
                } else {
                    logger.error (message, ex)
                }
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

    /**
     * Wrapper for Long that makes it possible to convert
     * it to TimeUnit object.
     */
    final class TimeUnitFromNumberCreator (x : Long) {
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
        def days         = mk (x * 1000L * 1000L * 1000L * 60L * 60L * 24L)

	@inline
        def mk (nano : Long) = new TimeUnit (nano)
    }
}
