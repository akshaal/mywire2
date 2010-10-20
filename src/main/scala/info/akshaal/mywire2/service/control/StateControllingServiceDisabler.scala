/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package service.control

import info.akshaal.jacore.`package`._

/**
 * This trait provides a way to disable service for some period of time.
 *
 * This is part of StateMonitoringService and the reason for existance of this trait is
 * to separate service disabling code from StateMonitoringService making it easier to understand.
 *
 * @param T type of value controlled by state monitoring service
 */
private[control] trait StateControllingServiceDisabler [T] {
    this : StateControllingService[T] =>

    // flag is true when state chaning is disabled due to frequent error or something
    private var disabled = false

    /**
     * Disable updates for the given period of time.
     *
     * @param period disable any updates for the given amount of time
     * @param onEnable code to run when it is time to enable updates
     */
    private[control] def disable (period : TimeValue, onEnable : => Unit) : Unit = {
        if (disabled) {
            error (serviceName +:+ "The service is already disabled!")
            return
        }

        // Remember that we are disabled
        disabled = true

        // Schedule some code to enable this service again after period is ended
        schedule in period executionOf {
            // Notify about end of disabled period
            onEnable

            // Reset flag
            disabled = false

            // Set new state
            updateState ()
        }
    }

    /**
     * Returns true if the service id disabled at the moment.
     *
     * @return true if disabled
     */
    private[control] def isDisabled = disabled
}
