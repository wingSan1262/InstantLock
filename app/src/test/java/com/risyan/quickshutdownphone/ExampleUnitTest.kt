package com.risyan.quickshutdownphone

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testOne(

    ){
        val a: List<Int> = listOf(1,2,3,4,5,6,7,8,9,10)
        val targetSum : Int = 12

        var counter = 0;
        val map = mutableMapOf<String, Int>()
        a.forEachIndexed { aIndex, i ->
            a.forEachIndexed { bIndex, i ->
                if(aIndex != bIndex){
                    val sum = a[aIndex] + a[bIndex]

                }
            }
        }
        assertEquals(4, counter)
    }
}