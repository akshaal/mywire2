/*
 * Implicits.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2

object Predefs {
    def mkRunnable (code : => Unit) = {
        new Runnable () {
            def run () {
                code
            }
        }
    }
}
