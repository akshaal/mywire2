/*
 * Module.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package module

import com.google.inject.name.Names

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.module.{Module => JacoreModule}

import utils.NativeThreadPriorityChanger

/**
 * This module is supposed to help instantiate all classes needed for mywire
 * to work.
 */
class Module extends JacoreModule {
    override lazy val prefsResource = "mywire.properties"

    override lazy val threadPriorityChangerImplClass = classOf [NativeThreadPriorityChanger]

    lazy val qosInterval = prefs.getTimeValue ("mywire.qos.interval")

    lazy val lowOsPriority = prefs.getInt ("mywire.os.priority.low")
    lazy val normalOsPriority = prefs.getInt ("mywire.os.priority.normal")
    lazy val hiOsPriority = prefs.getInt ("mywire.os.priority.hi")
    lazy val version = prefs.getString ("version")
    lazy val daemonJmxName = prefs.getString("mywire.daemon.jmx.name")

    // - - - - - - - - - - - - Bindings - - - - - - - - - -

    override def configure () = {
        super.configure ()

        bind (classOf[MywireManager]).to (classOf[MywireManagerImpl])

        //  - - - - - - - - - - -  Named - - - - - - - - - -  - - - -

        bind (classOf[TimeValue])
              .annotatedWith (Names.named ("mywire.qos.interval"))
              .toInstance (qosInterval)

        bind (classOf[Int])
              .annotatedWith (Names.named ("mywire.os.priority.low"))
              .toInstance (lowOsPriority)

        bind (classOf[Int])
              .annotatedWith (Names.named ("mywire.os.priority.normal"))
              .toInstance (normalOsPriority)

        bind (classOf[Int])
              .annotatedWith (Names.named ("mywire.os.priority.hi"))
              .toInstance (hiOsPriority)

        bind (classOf[String])
              .annotatedWith (Names.named ("version"))
              .toInstance (version)

        bind (classOf[String])
              .annotatedWith (Names.named ("mywire.daemon.jmx.name"))
              .toInstance (daemonJmxName)
    }
}
