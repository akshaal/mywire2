/*
 * FileActor.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system
package fs

import java.nio.channels.AsynchronousFileChannel
import java.nio.file.{Path, OpenOption}
import java.io.{File, IOException}
import java.nio.file.StandardOpenOption.{READ, WRITE, CREATE, TRUNCATE_EXISTING}
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset

import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named

import Predefs._
import actor.Actor
import logger.Logging
import utils.NormalPriorityPool
import scheduler.Scheduler

abstract sealed class FileMessage extends NotNull

final case class WriteFile (file : File,
                            content : String,
                            payload : Any)
                           extends FileMessage

final case class ReadFile (file : File,
                           payload : Any)
                            extends FileMessage

final case class WriteFileDone (file : File,
                                payload : Any)
                            extends FileMessage

final case class WriteFileFailed (file : File,
                                  exc : Throwable,
                                  payload : Any)
                            extends FileMessage

final case class ReadFileDone (file : File,
                               content : String,
                               payload : Any)
                            extends FileMessage

final case class ReadFileFailed (file : File,
                                 exc : Throwable,
                                 payload : Any)
                            extends FileMessage

// ///////////////////////////////////////////////////////////////////////
// File actor

/**
 * Fast async file reader/writer. Can read only limited number of bytes.
 */
@Singleton
private[system] final class FileActor @Inject() (
                       pool : NormalPriorityPool,
                       scheduler : Scheduler,
                       @Named("jacore.file.buffer.limit") readBytesLimit : Int,
                       prefs : Prefs)
                    extends Actor (pool = pool,
                                   scheduler = scheduler)
{
    private val encoding = prefs.getString("jacore.os.file.encoding")
    private val encoder = Charset.forName(encoding).newEncoder()
    private val decoder = Charset.forName(encoding).newDecoder()

    /**
     * Process actor message.
     */
    protected override final def act () = {
        case WriteFile(file, content, payload) => writeFile (file,
                                                             content,
                                                             payload)

        case ReadFile(file, payload) => readFile (file, payload)
    }

    /**
     * Open file and initiate writing to the file.
     */
    private def writeFile (file : File, content : String, payload : Any) =
    {
        val buf = encoder.encode(CharBuffer.wrap (content))
        val handler = new WriteCompletionHandler (buf, file, sender, payload)

        try {
            val ch = AsynchronousFileChannel.open (file.toPath,
                                                   WRITE,
                                                   CREATE,
                                                   TRUNCATE_EXISTING)
            handler.setChannel (ch)

            ch.write (buf, 0, null, handler)
        } catch {
            case exc : IOException => handler.failed (exc, null)
        }
    }

    /**
     * Open file and initiate reading from the file.
     */
    private def readFile (file : File, payload : Any) =
    {
        val buf = ByteBuffer.allocate (readBytesLimit)
        val handler = new ReadCompletionHandler (buf, file, sender, payload)

        try {
            val ch = AsynchronousFileChannel.open (file.toPath, READ)
            handler.setChannel (ch)

            ch.read (buf, 0, null, handler)
        } catch {
            case exc : IOException => handler.failed (exc, null)
        }
    }

    // //////////////////////////////////////////////////////////////////////
    // Write completion hander

    /**
     * Write completion handler
     */
    private [fs] final class WriteCompletionHandler (buf : ByteBuffer,
                                                     file : File,
                                                     sender : Option[Actor],
                                                     payload : Any)
                      extends CompletionHandler [java.lang.Integer, Object]
                      with Logging
    {
        private val bufLen = buf.remaining
        private var channel : AsynchronousFileChannel = null

        /**
         * Associate a channel this handler serves.
         */
        def setChannel (ch : AsynchronousFileChannel) = channel = ch

        /**
         * Called when write operation is finished.
         */
        override def completed (bytes : java.lang.Integer,
                                ignored : Object) : Unit = {
            if (bufLen != bytes) {
                failed (new IOException ("Only " + bytes
                                         + " number of bytes out of "
                                         + bufLen + " has been written for file "
                                         + file + " with payload " + payload),
                        null)
            } else {
                sender.foreach (_ ! (WriteFileDone (file, payload)))

                closeChannel ()
            }
        }

        /**
         * Called when write operation failed.
         */
        override def failed (exc : Throwable, ignored : Object) : Unit = {
            sender match {
                case None =>
                    error ("Failed to write to file " + file
                           + " with payload " + payload,
                           exc)

                case Some (actor) =>
                    actor ! (WriteFileFailed (file, exc, payload))
            }

            closeChannel ()
        }

        /**
         * Called when write operation is canceled. This must not happen.
         */
        override def cancelled (ignored : Object) : Unit = {
            throw new RuntimeException ("Impossible")
        }

        def closeChannel () = {
            if (channel != null) {
                logIgnoredException ("unable to close channel of file "
                                     + file + " with payload " + payload) {
                    channel.close ()
                }
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////
    // Read completion hander

    /**
     * Write completion handler
     */
    private [fs] final class ReadCompletionHandler (buf : ByteBuffer,
                                                    file : File,
                                                    sender : Option[Actor],
                                                    payload : Any)
                        extends CompletionHandler [java.lang.Integer, Object]
                        with Logging
    {
        private var channel : AsynchronousFileChannel = null

        /**
         * Associate a channel this handler serves.
         */
        def setChannel (ch : AsynchronousFileChannel) = channel = ch

        /**
         * Called when write operation is finished.
         */
        override def completed (bytes : java.lang.Integer,
                                ignored : Object) : Unit = {
            val content : String = decoder.decode (buf).toString

            sender.foreach (_ ! (ReadFileDone (file, content, payload)))
            closeChannel ()
        }

        /**
         * Called when write operation failed.
         */
        override def failed (exc : Throwable, ignored : Object) : Unit = {
            sender match {
                case None =>
                    error ("Failed to read from file: " + file
                           + " with payliad " + payload, exc)

                case Some (actor) =>
                    actor ! (ReadFileFailed (file, exc, payload))
            }

            closeChannel ()
        }

        /**
         * Called when read operation is canceled. This must not happen.
         */
        override def cancelled (ignored : Object) : Unit = {
            throw new RuntimeException ("Impossible")
        }

        def closeChannel () = {
            if (channel != null) {
                logIgnoredException ("unable to close channel of file: "
                                     + file + " with payliad " + payload) {
                    channel.close ()
                }
            }
        }
    }
}

