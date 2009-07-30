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

import Predefs._
import actor.Actor
import system.RuntimeConstants
import logger.Logging
import utils.NormalPriorityPool

/**
 * Fast async file reader/writer. Can read only limited number of bytes.
 */
private[system] trait FileActor extends Actor {
    protected val pool : NormalPriorityPool

    // - - - - - - - - - - - - - - -- - - - -- -  - -
    // Concrete

    private val encoder =
        Charset.forName(RuntimeConstants.fileEncoding).newEncoder()

    /**
     * Process actor message.
     */
    override final def act () = {
        case WriteFile(file, content) => writeFile (file, content)
    }

    /**
     * Open file and initiate writing to the file.
     */
    private def writeFile (file : File, content : String) = {
        val buf = encoder.encode(CharBuffer.wrap (content))
        val handler = new WriteCompletionHandler (buf, file, sender)

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
}

/**
 * Write completion handler
 */
private [fs] final class WriteCompletionHandler (buf : ByteBuffer,
                                                 file : File,
                                                 sender : Option[Actor])
                       extends CompletionHandler [Integer, Object]
                       with Logging
{
    val bufLen = buf.remaining
    var channel : AsynchronousFileChannel = null

    /**
     * Associate a channel this handler serves.
     */
    def setChannel (ch : AsynchronousFileChannel) =
        channel = ch

    /**
     * Called when write operation is finished.
     */
    override def completed (bytes : java.lang.Integer,
                            ignored : Object) : Unit = {
        if (bufLen != bytes) {
            failed (new IOException ("Only " + bytes
                                     + " number of bytes out of "
                                     + bufLen + " has been written for file"
                                     + file),
                    null)
        } else {
            sender.foreach (_ ! (WriteFileDone (file)))

            closeChannel ()
        }
    }

    /**
     * Called when write operation failed.
     */
    override def failed (exc : Throwable, ignored : Object) : Unit = {
        sender match {
            case None =>
                error ("Failed to write to file: " + file, exc)

            case Some (actor) =>
                actor ! (WriteFileFailed (file, exc))
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
            logIgnoredException ("unable to close channel of file: " + file) {
                channel.close ()
            }
        }
    }
}

private[system] abstract sealed class FileMessage

private[system] final case class WriteFile (file : File, content : String)
                           extends FileMessage

private[system] final case class ReadFile (name : String)
                            extends FileMessage

private[system] final case class WriteFileDone (file : File)
                            extends FileMessage

private[system] final case class WriteFileFailed (file : File, exc : Throwable)
                            extends FileMessage

private[system] final case class ReadFileDone (file : File)
                            extends FileMessage

private[system] final case class ReadFileFailed (file : File, exc : Throwable)
                            extends FileMessage