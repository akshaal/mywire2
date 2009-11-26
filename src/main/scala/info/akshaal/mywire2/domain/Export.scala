/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.system.domain

/**
 * An object extending this trait is supposed to be exported outside of this application
 * (using JMS or other transport).
 */
trait Export {
    /**
     * Returns this object as a map.
     */
    def toMap : Map[String, Any] = {
        Map (("type", this.getClass.getName))
    }
}
