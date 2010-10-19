/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package integration

import org.apache.ibatis.session.Configuration

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.utils.io.IbatisUtils._

import domain.LogRecordLevel

object MywireIbatisConfigurer {
    /**
     * Setup IBatis configuration for Mywire2.
     */
    def configure (configuration : Configuration) {
        configuration.parseMapperXmlsInPackages ("info.akshaal.mywire2.sqlmaps")
        configuration.addTypeHandler (LogRecordLevel, classOf[LogRecordLevel.Level])
    }
}
