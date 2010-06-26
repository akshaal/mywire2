/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.utils.container

/**
 * Read-write container for a state of type T.
 */
trait RWStateContainer[T] extends ReadableStateContainer[T] with WriteableStateContainer[T]
