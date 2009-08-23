/*
 * Module.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system
package module

import com.google.inject.{Module => GuiceModule, Binder,
                          Singleton, Inject}
import com.google.inject.name.Names

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.system.utils.ThreadPriorityChanger
import info.akshaal.jacore.system.module.{Module => JacoreModule}

import utils.NativeThreadPriorityChanger

/**
 * This module is supposed to help instantiate all classes needed for mywire
 * to work.
 */
class Module extends JacoreModule {
    override lazy val prefsResource = "/mywire.properties"

    // - - - - - - - - - - - - Bindings - - - - - - - - - -

    override def configure (binder : Binder) = {
        super.configure (binder)

        binder.bind (classOf[ThreadPriorityChanger])
              .to (classOf[NativeThreadPriorityChanger])
    }
}