/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

/**
 * Describes a problem.
 */
trait Problem {
    /**
     * Returns mesage describing the problem if problem is detected, or None otherwise.
     */
    def detected () : Option[String]

    /**
     * Returns message describing new positive state if problem is gone, or None otherwise.
     */
    def isGone () : Option[String]
}
