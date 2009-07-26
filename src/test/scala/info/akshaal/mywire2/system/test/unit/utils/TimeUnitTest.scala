/**
 * Akshaal (C) 2009. GNU GPL. http://akshaal.info
 */

package info.akshaal.mywire2.system.test.unit.utils

import mywire2.Predefs._
import mywire2.system.utils.TimeUnit
import mywire2.system.test.unit.BaseUnitTest

import org.testng.annotations.Test
import org.testng.Assert.{assertEquals, assertTrue}

class TimeUnitTest extends BaseUnitTest {
    @Test (groups = Array("unit"))
    def testConstruction () = {
        assertEquals (10.nanoseconds.asNanoseconds, 10L)
        assertEquals (11.microseconds.asNanoseconds, 11L * 1000L)
        assertEquals (12.milliseconds.asNanoseconds, 12L * 1000L * 1000L)
        assertEquals (13.seconds.asNanoseconds, 13L * 1000L * 1000L * 1000L)
        assertEquals (14.minutes.asNanoseconds, 14L * 1000L * 1000L * 1000L * 60L)
        assertEquals (15.hours.asNanoseconds, 15L * 1000L * 1000L * 1000L * 60L * 60L)
    }

    @Test (groups = Array("unit"))
    def testEquals () = {
        assertEquals (1.milliseconds, 1.milliseconds)
        assertTrue (1.milliseconds != 10.milliseconds)
        assertEquals (2.hours, 2.hours)
        assertTrue (2.hours != 1.hours)
        assertEquals (3.minutes, 3.minutes)
        assertTrue (3.minutes != 1.minutes)
        assertEquals (30.seconds, 30.seconds)
        assertEquals (30.seconds, 30000.milliseconds)
        assertTrue (30.seconds != 1.seconds)
        assertTrue (30.seconds != 30.minutes)
    }

    @Test (groups = Array("unit"))
    def testArith () = {
        assertEquals (23.hours + 1.hours, 24.hours)
        assertEquals (50.minutes + 10.minutes + 3.hours, 4.hours)
        assertEquals (60.seconds + 1.minutes, 120.seconds)
    }

    def testToString () = {
        assertEquals (48.hours + 23.hours + 1.minutes + 45.seconds
                      + 15.milliseconds + 10.microseconds + 100.nanoseconds,
                      "2days 23hours 1mins 45secs 15ms 10us 100ns")

        assertEquals (2.hours + 5.seconds + 7.microseconds, "2hours 5secs 7us")

        assertEquals (11.minutes + 33.milliseconds + 55.nanoseconds,
                      "11mins 33ms 35ns")

        assertEquals (60.seconds, "1mins")
        
        assertEquals (0.seconds, "0ns")
    }
}