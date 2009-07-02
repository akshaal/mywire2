/*
 * Implicits.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2

import info.akshaal.mywire2.logger.Logger

object Predefs {
    /**
     * Create object of interface Runnable which will execute the given
     * block of code.
     */
    def mkRunnable (code : => Unit) = {
        new Runnable () {
            def run () {
                code
            }
        }
    }

    /**
     * Execute block of code and print a message if block of code throws
     * an exception. If logger is not null, then logger is used to print
     * the message, otherwise message is shown through stderr stream.
     */
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
}
