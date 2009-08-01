/*
 * Prefs.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package info.akshaal.mywire2

import java.util.Properties

import Predefs._

final class Prefs (file : String) {
    private[this] val properties = new Properties ()

    withCloseableIO {
        convertNull (this.getClass.getResourceAsStream (file)) {
            throw new IllegalArgumentException ("Failed to find preferences: "
                                                + file)
        }
    } {
        properties.load (_)
    }

    final def getString (name : String) : String = {
        convertNull (properties.getProperty (name)) {
            throw new IllegalArgumentException ("Property "
                                                + name
                                                + " is required")
        }
    }

    final def getInt (name : String) : Int = {
        Integer.valueOf(getString (name)).intValue
    }
}
