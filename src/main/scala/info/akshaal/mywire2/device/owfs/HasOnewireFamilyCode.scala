/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

/**
 * For devices that are support family code.
 */
trait HasOnewireFamilyCode {
    val familyCode : String
}