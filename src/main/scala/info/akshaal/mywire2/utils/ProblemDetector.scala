/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils

/**
 * Describes a problem detector.
 */
trait ProblemDetector {
    /**
     * Returns mesage describing the problem if problem is detected, or None otherwise.
     *
     * @return None if problem is absent, or Some with message describing current state
     */
    def detected () : Option[String]

    /**
     * Returns message describing new positive state if problem is gone, or None otherwise.
     *
     * @return None if problem is still present, or Some (with message) if problem is gone
     */
    def isGone () : Option[String]
}
