/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.domain

case class Temperature (name : String, value : Double, average3 : Double) extends Export {
    override def toMap : Map[String, Any] = {
        super.toMap
             .updated ("name", name)
             .updated ("value", value)
             .updated ("average3", average3)
    }
}
