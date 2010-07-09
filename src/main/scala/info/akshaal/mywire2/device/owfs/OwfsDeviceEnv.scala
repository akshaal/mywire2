/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2
package device
package owfs

import info.akshaal.jacore.`package`._
import info.akshaal.jacore.actor.HiPriorityActorEnv
import info.akshaal.jacore.fs.text.TextFile

/**
 * Device environment.
 */
@Singleton
class OwfsDeviceEnv @Inject() (val actorEnv : HiPriorityActorEnv,
                               val textFile : TextFile)
                    extends NotNull