/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device

/**
 * Class describes device location in the filesystem.
 */
sealed case class DeviceLocation (absolutePath : String) extends NotNull {
    def this (parent : DeviceLocation, relativePath : String) =
            this (parent.absolutePath + "/" + relativePath)
}
