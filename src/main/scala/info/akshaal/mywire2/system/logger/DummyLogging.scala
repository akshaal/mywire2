/*
 * Logging.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.logger

trait DummyLogging extends Logging {
    protected[logger] override val logger = DummyLogger
}
