/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package utils
package container

import info.akshaal.jacore.actor.Operation

/**
 * Container that is able to provide a humidity.
 */
trait HumidityContainer {
    def opReadHumidity () : Operation.WithResult [Double]
}
