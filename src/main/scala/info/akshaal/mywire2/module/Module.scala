/*
 * Module.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package module

import com.google.inject.Binder
import com.google.inject.name.Names

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.module.{Module => JacoreModule}

import utils.NativeThreadPriorityChanger

/**
 * This module is supposed to help instantiate all classes needed for mywire
 * to work.
 */
class Module extends JacoreModule {
    override lazy val prefsResource = "mywire.properties"

    override lazy val threadPriorityChangerImplClass = classOf [NativeThreadPriorityChanger]

    lazy val qosInterval = prefs.getTimeUnit ("mywire.qos.interval")

    lazy val lowOsPriority = prefs.getInt ("mywire.os.priority.low")
    lazy val normalOsPriority = prefs.getInt ("mywire.os.priority.normal")
    lazy val hiOsPriority = prefs.getInt ("mywire.os.priority.hi")

    // - - - - - - - - - - - - Bindings - - - - - - - - - -

    override def configure (binder : Binder) = {
        super.configure (binder)

        binder.bind (classOf[MywireManager]).to (classOf[MywireManagerImpl])

        //  - - - - - - - - - - -  Named - - - - - - - - - -  - - - -

        binder.bind (classOf[TimeUnit])
              .annotatedWith (Names.named ("mywire.qos.interval"))
              .toInstance (qosInterval)

        binder.bind (classOf[Int])
              .annotatedWith (Names.named ("mywire.os.priority.low"))
              .toInstance (lowOsPriority)

        binder.bind (classOf[Int])
              .annotatedWith (Names.named ("mywire.os.priority.normal"))
              .toInstance (normalOsPriority)

        binder.bind (classOf[Int])
              .annotatedWith (Names.named ("mywire.os.priority.hi"))
              .toInstance (hiOsPriority)
    }
}
