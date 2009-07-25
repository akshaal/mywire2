/** Akshaal (C) 2009. GNU GPL. http://akshaal.info */

package info.akshaal.mywire2.system.daemon

/**
 * Daemon.
 */
class Daemon {
    /**
     * Called by native executable to initialize the application before starting it.
     */
    def init () = ()

    /**
     * Called by native executable to start the application after the application
     * has been initialized.
     */
    def start () = ()

    /**
     * Called by native executable to stop the application before destroying it.
     */
    def stop () = ()

    /**
     * Called by native executable to destroy the application.
     */
    def destroy () = ()
}