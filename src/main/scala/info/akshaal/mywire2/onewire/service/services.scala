/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package onewire
package service

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.{Actor, HiPriorityActorEnv}
import info.akshaal.jacore.scheduler.ScheduleControl
import info.akshaal.jacore.utils.OptionDoubleValueFrame

import device.{DeviceHasTemperature, DeviceHasHumidity}
import domain.{Temperature, Humidity, StateUpdated, StateSensed}
import utils.{StateUpdate, Problem}
import utils.container._

/**
 * Reads temperature value from device and broadcasts it as exportable object.
 */
class TemperatureMonitoringService (actorEnv : HiPriorityActorEnv,
                                    temperatureDevice : DeviceHasTemperature,
                                    name : String,
                                    interval : TimeValue,
                                    illegalTemperature : Option[Double] = None,
                                    maxTries : Int = 3)
                     extends Actor (actorEnv = actorEnv)
{
    // Frame to keep average of temperature values
    private val frame = new OptionDoubleValueFrame (3)

    // How many tries we can perform in case of error, None means that no errors happened yet.
    private var triesLeft : Option[Int] = None

    // Local function to bradcast temperature
    private def broadcastTemperature (temperatureValue : Option[Double]) : Unit = {
        frame.put (temperatureValue)

        val temperature = new Temperature (name = name,
                                           value = temperatureValue,
                                           average3 = frame.average)
        broadcaster.broadcast (temperature)
    }

    // Schedule updates
    schedule every interval executionOf readTemperature()

    // Read temperature
    protected def readTemperature () : Unit =
        temperatureDevice.opReadTemperature () runMatchingResultAsy {
            case Success (temperatureValue) =>
                if (Some (temperatureValue) == illegalTemperature) {
                    warn ("Illegal temperature got from " + temperatureDevice + "" + illegalTemperature.get)

                    triesLeft match {
                        case None =>
                            triesLeft = Some (maxTries - 1) // First try
                            readTemperature ()

                        case Some (tries) if tries > 1 =>
                            triesLeft = Some (tries - 1) // We have some tries left
                            readTemperature ()

                        case _ =>
                            triesLeft = None // Can't try anymore
                            broadcastTemperature (None)
                    }
                } else {
                    triesLeft = None // Everything is fine, no need to try
                    broadcastTemperature (Some (temperatureValue))
                }

            case Failure (exc) =>
                triesLeft = None // Something is bad, we can't try
                error ("Error reading temperature of " + temperatureDevice + ": " + exc.getMessage, exc)
                broadcastTemperature (None)
        }

    override def toString() : String = {
        getClass.getSimpleName + "(name=" + name +
            ", temperatureDevice=" + temperatureDevice + ", interval=" + interval + ")"
    }
}

/**
 * Read humidity value from device and broadcasts it as exportable object.
 */
class HumidityMonitoringService (actorEnv : HiPriorityActorEnv,
                                 humidityDevice : DeviceHasHumidity,
                                 name : String,
                                 interval : TimeValue)
                     extends Actor (actorEnv = actorEnv)
{
    private val frame = new OptionDoubleValueFrame (3)

    // Load function to broadcast humidity
    private def broadcastHumidity (humidityValue : Option[Double]) : Unit = {
        frame.put (humidityValue)

        val humidity = new Humidity (name = name, value = humidityValue, average3 = frame.average)
        broadcaster.broadcast (humidity)
    }

    schedule every interval executionOf {
        humidityDevice.opReadHumidity () runMatchingResultAsy {
            case Success (humidityValue) =>
                broadcastHumidity (Some (humidityValue))

            case Failure (exc) =>
                error ("Error reading humidity of " + humidityDevice + ": " + exc.getMessage, exc)
                broadcastHumidity (None)
        }
    }

    override def toString() : String = {
        getClass.getSimpleName + "(name=" + name +
            ", humidityDevice=" + humidityDevice + ", interval=" + interval + ")"
    }
}

/**
 * Read state value from state container and broadcasts it as exportable object.
 */
class StateMonitoringService[T] (actorEnv : HiPriorityActorEnv,
                                 stateContainer : ReadableStateContainer[T],
                                 name : String,
                                 interval : TimeValue)
                     extends Actor (actorEnv = actorEnv)
{
    // Load function to broadcast humidity
    private def broadcastSensed (state : Option[T]) : Unit = {
        val msg = new StateSensed (name = name, value = state)
        broadcaster.broadcast (msg)
    }

    schedule every interval executionOf {
        stateContainer.opGetState () runMatchingResultAsy {
            case Success (stateValue) =>
                broadcastSensed (Some(stateValue))

            case Failure (exc) =>
                error ("Error reading state of " + stateContainer + ": " + exc.getMessage, exc)
                broadcastSensed (None)
        }
    }

    override def toString() : String = {
        getClass.getSimpleName + "(name=" + name +
            ", humidityDevice=" + stateContainer + ", interval=" + interval + ")"
    }
}

/**
 * A service to control some state.
 * @param actorEnv actor environment
 * @param stateContainer state container
 * @param name name
 * @param interval interval to check and update state
 * @param tooManyProblemsNumber if number of problems within tooManyProblemsInterval
 *              is greater or equal to this number, then service is disabled (in safe mode)
 * @param tooManyProblemsInterval see tooManyProblemsNumber
 * @param disableOnTooManyProblemsFor if too many problems detected, then the service is disabled
 *                     for this amount of time
 */
abstract class StateControllingService [T] (actorEnv : HiPriorityActorEnv,
                                            stateContainer : WriteableStateContainer [T],
                                            name : String,
                                            interval : TimeValue = 10 seconds,
                                            tooManyProblemsNumber : Int = 5,
                                            tooManyProblemsInterval : TimeValue = 10 minutes,
                                            disableOnTooManyProblemsFor : TimeValue = 15 minutes)
                            extends Actor (actorEnv = actorEnv)
{
    private var earlyUpdateControl : Option[ScheduleControl] = None
    private var previousState : Option[T] = None
    private var currentProblem : Option[Problem] = None
    private var problemEndHistory : List[TimeValue] = Nil
    private var disabledUntil : Option[TimeValue] = None

    schedule every interval executionOf updateState()

    /**
     * Update state if state is changed.
     */
    def updateStateIfChanged () : Unit = {
        postponed {
            updateState (onlyIfChanged = true)
        }
    }
    
    /**
     * Update state.
     */
    private def updateState (onlyIfChanged : Boolean = false) : Unit = {
        earlyUpdateControl foreach (_.cancel ())
        earlyUpdateControl = None

        // Check if we are disabled
        for (time <- disabledUntil) {
            if (System.nanoTime.nanoseconds > time) {
                onTooManyProblemsExpired ()
                disabledUntil = None
            }
        }

        // If not disabled, check for problems
        if (disabledUntil == None) {
            for (problem <- currentProblem) {
                // Problem was detected previous time, so just check if it is disappeared
                for (goneMsg <- problem.isGone) {
                    onProblemGone (problem)
                    currentProblem = None

                    // We have to check if too many problems occured within interval
                    problemEndHistory = System.nanoTime.nanoseconds :: problemEndHistory
                    problemEndHistory = problemEndHistory.filter(_ + tooManyProblemsInterval > System.nanoTime.nanoseconds)
                    if (problemEndHistory.size >= tooManyProblemsNumber) {
                        onTooManyProblems ()
                        disabledUntil = Some (System.nanoTime.nanoseconds + disableOnTooManyProblemsFor)
                        schedule in disableOnTooManyProblemsFor executionOf updateState()
                    }
                }
            }

            if (currentProblem == None) {
                for (newProblem <- possibleProblems.find (_.detected != None)) {
                    onProblem (newProblem)
                    currentProblem = Some (newProblem)
                }
            }
        }

        // Check for silent problems
        lazy val foundSilentProblems = possibleSilentProblems.find(_.detected != None)
        if (isDebugEnabled) {
            for (silentProblem <- foundSilentProblems) {
                debug ("Found silent problem: " + silentProblem.detected)
            }
        }

        // Get new state
        val stateUpdate =
            if (currentProblem == None && disabledUntil == None && foundSilentProblems == None)
                getStateUpdate ()
            else
                new StateUpdate (state = safeState, validTime = interval * 3)
            
        val newState = stateUpdate.state
        val newValidTime = stateUpdate.validTime

        // Exit from method if onlyIfChanged flag set and state is not changed
        if (onlyIfChanged) {
            for (state <- previousState) {
                if (state == newState) {
                    return
                }
            }
        }

        // Change state
        stateContainer.opSetState (newState) runMatchingResultAsy {
            case Success (_) =>
                val stateUpdated = new StateUpdated (name = name, value = newState)
                broadcaster.broadcast (stateUpdated)
            
                previousState = Some (newState)

            case Failure (exc) =>
                error ("Error setting state " + newState + " of " + stateContainer + ": " + exc.getMessage, exc)
        }

        // Schedule next state update
        if (newValidTime > (0 nanoseconds) && newValidTime < (interval * 2)) {
            earlyUpdateControl = Some (schedule in newValidTime executionOf updateState())
        } else {
            earlyUpdateControl = None
        }
    }

    override def start () : Boolean = {
        val started = super.start ()

        if (started) {
            postponed {
                updateState ()
            }
        }

        started
    }

    /**
     * Called when a problem detected.
     */
    protected def onProblem (problem : Problem) {
        error (name + ": Problem detected: " + problem.detected.get)
    }

    /**
     * Called when a problem is gone.
     */
    protected def onProblemGone (problem : Problem) {
        info (name + ": Problem gone: " + problem.isGone.get)
    }

    /**
     * Called when too many problems detected.
     */
    protected def onTooManyProblems () {
        error (name + ": Too many problems occured within last " + tooManyProblemsInterval
               + ". Service will be switched into safe mode for the next "
               + disableOnTooManyProblemsFor)
    }

    /**
     * Called when service is switched back online after too many problems.
     */
    protected def onTooManyProblemsExpired () {
        info (name + ": Service is back online after too many problems expired")
    }

    /**
     * Get new state.
     */
    protected def getStateUpdate () : StateUpdate[T]

    /**
     * Returns state that is considered safe for human environment.
     */
    protected def safeState : T

    /**
     * List of possible problems.
     */
    protected def possibleProblems : List [Problem]

    /**
     * List of possible problem that don't generate any error messages, but just
     * switches device to the safe mode.
     */
    protected def possibleSilentProblems : List[Problem]
}