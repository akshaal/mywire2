/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service.control

import info.akshaal.jacore.`package`._

import utils.ProblemDetector

/**
 * Management for problem that may be detected during work of the StateMonitoringService.
 * This is part of StateMonitoringService and the reason for existance of this trait is
 * to separate problem oriented code from StateMonitoringService making it easier to understand.
 *
 * @param T type of value controlled by state monitoring service
 */
private[control] trait StateControllingServiceProblemManager[T] {
    this : StateControllingService[T] =>

    // reference to a problem detector that has detected a problem (and the problem is not yet gone)
    private var currentProblemDetector : Option[ProblemDetector] = None

    // each time a problem disappears, a current time is added to the list
    private var problemEndHistory : List[TimeValue] = Nil

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Customization of behavior

    /**
     * List of problem detectors.
     */
    protected val problemDetectors : List [ProblemDetector] = Nil

    /**
     * List of problem detectors that don't generate any error messages, but just
     * switches device to the safe mode.
     */
    protected val silentProblemDetectors : List [ProblemDetector] = Nil

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Reactions on events on actions

    /**
     * Called when a problem detected.
     *
     * @param problemDetector problem detector that has detected a problem
     */
    protected def onProblem (problemDetector : ProblemDetector) : Unit = {
        businessLogicProblem (serviceName +:+ "Problem detected" +:+ problemDetector.detected.get)
    }

    /**
     * Called when a problem is gone.
     *
     * @param problemDetector problem detector that detected the problem that is currently gone
     */
    protected def onProblemGone (problemDetector : ProblemDetector) : Unit = {
        businessLogicInfo (serviceName +:+ "Problem gone" +:+ problemDetector.isGone.get)
    }

    /**
     * Called when too many problems detected.
     */
    protected def onTooManyProblems () : Unit = {
        businessLogicProblem (serviceName +:+ "Too many problems occured within last "
                              + tooManyProblemsInterval
                              + ". Service will be switched into safe mode for the next "
                              + disableOnTooManyProblemsFor)
    }

    /**
     * Called when service is switched back online after too many problems.
     */
    protected def onTooManyProblemsExpired () : Unit = {
        businessLogicInfo (serviceName +:+ "Service is back online after too many problems expired")
    }

    // ===========================================================================
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Internals

    /**
     * Checks if previously detected problem has disappeared.
     *
     * This method does nothing if no problem is previously detected.
     */
    private def detectProblemDisappearance () : Unit = {
        if (isDisabled) {
            return
        }

        for (problemDetector <- currentProblemDetector) {
            // Problem was detected previous time, so just check if it is disappeared
            for (goneMsg <- problemDetector.isGone) {
                onProblemGone (problemDetector)
                currentProblemDetector = None

                // We have to check if too many problems occured within interval
                problemEndHistory ::= System.nanoTime.nanoseconds
                problemEndHistory =
                    problemEndHistory.filter(_ + tooManyProblemsInterval > System.nanoTime.nanoseconds)

                if (problemEndHistory.size >= tooManyProblemsNumber) {
                    onTooManyProblems ()

                    disable (disableOnTooManyProblemsFor, onEnable = onTooManyProblemsExpired ())
                }
            }
        }
    }

    /**
     * Detect problems.
     *
     * This method does nothing if a problem is already detected.
     */
    private def detectProblem () : Unit = {
        if (isDisabled) {
            return
        }

        if (currentProblemDetector.isEmpty) {
            val allProblemDetectors = basicProblemDetectors ++ problemDetectors

            for (newProblemDetector <- allProblemDetectors.find (_.detected.isDefined)) {
                onProblem (newProblemDetector)
                currentProblemDetector = Some (newProblemDetector)
            }
        }
    }

    /**
     * Find silent problem.
     *
     * @return problem detector that detected a silent problem or None if everything is fine
     */
    private def findSilentProblem : Option [ProblemDetector] = {
        val allSilentProblemDetectors = basicSilentProblemDetectors ++ silentProblemDetectors
        val silentProblemDetectorOption = allSilentProblemDetectors.find (_.detected.isDefined)

        if (isDebugEnabled) {
            for (silentProblemDetector <- silentProblemDetectorOption) {
                debug ("Found silent problem" +:+ silentProblemDetector.detected)
            }
        }

        silentProblemDetectorOption
    }

    /**
     * Check for problems. Returns true if there is a problem.
     * @return true if a problem is found
     */
    private[control] def checkForProblems () : Boolean = {
        detectProblemDisappearance ()
        detectProblem ()

        currentProblemDetector.isDefined || findSilentProblem.isDefined
    }
}
