/*
 * FileActor.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.fs

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.io.{File, IOException}
import java.nio.file.StandardOpenOption.{READ, WRITE, CREATE, TRUNCATE_EXISTING}
import java.nio.{ByteBuffer, CharBuffer}
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset

import actor.HiPriorityActor
import system.RuntimeConstants

/**
 * Fast async file reader/writer. Can read only limited number of bytes.
 */
// TODO: NormalPriorityActor
private[system] object FileActor extends HiPriorityActor {
    start ()

    private val encoder =
        Charset.forName(RuntimeConstants.fileEncoding).newEncoder()

    /**
     * Process actor message.
     */
    override def act () = {
        // Write file
        case WriteFile(file, content) => writeFile (file, content)
    }

    /**
     * Open file and initiate writing to the file.
     */
    private def writeFile (file : File, content : String) = {
        val ch = AsynchronousFileChannel.open (file.toPath,
                                               WRITE,
                                               CREATE,
                                               TRUNCATE_EXISTING)
        val buf = encoder.encode(CharBuffer.wrap (content))
        val bufLen = buf.remaining
        val origSender = sender
        val handler = new CompletionHandler[Integer, Object] () {
            override def completed (bytes : java.lang.Integer,
                                    ignored : Object) : Unit = {
                if (bufLen != bytes) {
                    failed (new IOException ("Only " + bytes
                                             + " number of bytes out of "
                                             + bufLen + " has been written"),
                            null)
                } else {
                    // TODO: Measure time

                    if (origSender != null) {
                        origSender ! (WriteFileDone (file))
                    }

                    ch.close ()
                }
            }

            override def failed (exc : Throwable, ignored : Object) : Unit = {
                // TODO: Measure time

                if (origSender == null) {
                    error ("Failed to write to file: " + file, exc)
                } else {
                    origSender ! (WriteFileFailed (file, exc))
                }

                ch.close ()
            }

            override def cancelled (ignored : Object) : Unit = {
                throw new RuntimeException ("Impossible")
            }
        }

        ch.write (buf, 0, null, handler)
    }
}

// TODO: [system]
abstract sealed class FileMessage

// TODO: [system]
final case class WriteFile (file : File, content : String)
                           extends FileMessage

// TODO: [system]
final case class ReadFile (name : String)
                            extends FileMessage

// TODO: [system]
final case class WriteFileDone (file : File)
                            extends FileMessage

// TODO: [system]
final case class WriteFileFailed (file : File, exc : Throwable)
                            extends FileMessage

// TODO: [system]
final case class ReadFileDone (file : File)
                            extends FileMessage

// TODO: [system]
final case class ReadFileFailed (file : File, exc : Throwable)
                            extends FileMessage