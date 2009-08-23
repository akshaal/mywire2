/*
 * TestModule.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system.test.unit

import com.google.inject.{Guice, Injector}

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.system.actor.{Actor, HiPriorityActorEnv}
import info.akshaal.jacore.system.daemon.DaemonStatus

import system.MywireManager
import system.module.Module

object UnitTestModule extends Module {
    override lazy val daemonStatusJmxName = "mywire:name=testStatus"

    val injector = Guice.createInjector (this)
    val mywireManager = injector.getInstance (classOf[MywireManager])

    mywireManager.start

    val daemonStatus = injector.getInstance (classOf[DaemonStatus])
    val hiPriorityActorEnv = injector.getInstance (classOf[HiPriorityActorEnv])
}

abstract class HiPriorityActor extends Actor (
                            UnitTestModule.hiPriorityActorEnv)
