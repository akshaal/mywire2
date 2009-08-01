/*
 * TestModule.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package system.test.unit

import Predefs._
import system.actor.Actor
import system.module.Module

object UnitTestModule extends {
    override val daemonStatusJmxName = "mywire:name=testStatus"
} with Module {
    start
}

abstract class HiPriorityActor extends Actor (
                     scheduler = UnitTestModule.scheduler,
                     pool = UnitTestModule.hiPriorityPool)