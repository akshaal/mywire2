/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2
package daemon

import com.google.inject.Guice

import info.akshaal.jacore.Predefs._
import info.akshaal.jacore.module.Module

/**
 * Abstract daemon.
 *
 * @param Instantance of module that will is used for injector.
 */
class BaseDaemon (module : Module) {
    /**
     * Injector that is supposed to be used to instantiate all IoC classes of the app.
     */
    protected final val injector = Guice.createInjector (module)

    /**
     * MywireManager instance of the application.
     */
    protected final val mywireManager = injector.getInstanceOf [MywireManager]

    /**
     * Called by native executable to initialize the application before starting it.
     */
    def init () = ()

    /**
     * Called by native executable to start the application after the application
     * has been initialized.
     */
    def start () = {
        mywireManager.start
    }

    /**
     * Called by native executable to stop the application before destroying it.
     */
    def stop () = {
        mywireManager.stop
    }

    /**
     * Called by native executable to destroy the application.
     */
    def destroy () = ()
}
