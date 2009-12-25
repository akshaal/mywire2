/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.daemon

import info.akshaal.jacore.actor.Actor

/**
 * Actor classes marked with this interface will be registered in Guice Injector automatically.
 */
trait Autoregister {
    this : Actor =>

    def registrationName : String
}
