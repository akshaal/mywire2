/** Akshaal (C) 2009-2010. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2
package daemon

import java.util.HashMap

import javax.management.{ObjectName, MBeanServerConnection}
import javax.management.remote.{JMXServiceURL, JMXConnectorFactory}

import module.Module

/**
 * Client to restart daemon by using JMX.
 *
 * @author akshaal
 */
abstract class BaseDaemonJMXClient (module : Module) {
    protected val MAX_TRIES = 60 // one try per second

    protected val daemonUrl : String
    protected val jmxUser : String
    protected val jmxPassword : String

    // private static final String CONTROL_ROLE = "controlRole";

    protected def show (str : String) : Unit = {
        println (getClass.getSimpleName + ": " + str)
    }

    def run () : Unit = {
        // Daemon name
        val daemon = new ObjectName (module.daemonJmxName)

        // Old version
        val oldVersion = getServer ().getAttribute (daemon, "version").asInstanceOf [String]
        
        show ("Currently remotly running version: " + oldVersion)
        show ("Version we are installing: " + module.version)
        if (oldVersion == module.version) {
            show ("FAILURE: Versions are the same. Do nothing!")
            return
        }

        if (module.version.endsWith ("-SNAPSHOT")) {
            show ("FAILURE: It is illegal to install snapshots!")
            return
        }

        // Restart
        show ("Going to restart remote mywire2");
        getServer ().invoke (daemon, "restart", null, null)

        show ("Restarting:")

        // New version
        var tries = MAX_TRIES
        while (tries > 0) {
            tries -= 1

            // Delay
            Thread.sleep (1000)

            // Findout version
            try {
                val newVersion = getServer ().getAttribute (daemon, "version").asInstanceOf [String]
                if (newVersion == module.version) {
                    show ("SUCCESS!")
                    return
                } else {
                    show (".. still the same version..")
                }
            } catch {
                case ex : Exception =>
                    show (".. in progress ..")
            }
        }

        show ("FAILURE: time out")
    }

    /**
     * Get connection to the mbean server.
     * @return connection
     */
    protected def getServer () : MBeanServerConnection =
    {
        val url = new JMXServiceURL (daemonUrl)
        val credentials = Array[String] (jmxUser, jmxPassword)

        val props = new HashMap[String, Array[String]]
        props.put ("jmx.remote.credentials", credentials)

        return JMXConnectorFactory.connect (url, props).getMBeanServerConnection
    }
}
