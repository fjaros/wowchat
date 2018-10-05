package wowchat.realm

/*
 * Copyright 2016 Warkdev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.security.SecureRandom

/**
  * This class was taken from JaNGOS project and ported to Scala.
  * Look at JaNGOSAuth for further documentation about the algorithm used here
  *
  * https://github.com/Warkdev/JaNGOSAuth
  */
object BigNumber {

  def apply(value: BigInt): BigNumber = {
    new BigNumber(value)
  }

  def apply(value: String): BigNumber = {
    BigNumber(value, 16)
  }

  def apply(value: String, radix: Int): BigNumber = {
    new BigNumber(BigInt(value, radix))
  }

  def apply(array: Array[Byte], reverse: Boolean = false): BigNumber = {
    if (reverse) {
      val length = array.length
      (0 until length / 2).foreach(i => {
        val j = array(i)
        array(i) = array(length - i - 1)
        array(length - i - 1) = j
      })
    }

    if (array(0) < 0) {
      val tmp = new Array[Byte](array.length + 1)
      System.arraycopy(array, 0, tmp, 1, array.length)
      new BigNumber(BigInt(tmp))
    } else {
      new BigNumber(BigInt(array))
    }
  }

  def rand(amount: Int): BigNumber = {
    new BigNumber(BigInt(1, new SecureRandom().generateSeed(amount)))
  }
}

class BigNumber(private val bigInt: BigInt) {

  def *(number: BigNumber): BigNumber = {
    new BigNumber(bigInt * number.bigInt.abs)
  }

  def -(number: BigNumber): BigNumber = {
    new BigNumber(bigInt - number.bigInt.abs)
  }

  def +(number: BigNumber): BigNumber = {
    new BigNumber(bigInt + number.bigInt.abs)
  }

  def modPow(val1: BigNumber, val2: BigNumber): BigNumber = {
    new BigNumber(bigInt.modPow(val1.bigInt.abs, val2.bigInt.abs))
  }

  def toHexString: String = {
    bigInt.toString(16).toUpperCase
  }

  def asByteArray(reqSize: Int = 0, reverse: Boolean = true): Array[Byte] = {
    var array = bigInt.toByteArray
    if (array(0) == 0) {
      val tmp = new Array[Byte](array.length - 1)
      System.arraycopy(array, 1, tmp, 0, tmp.length)
      array = tmp
    }

    val length = array.length
    if (reverse) {
      (0 until length / 2).foreach(i => {
        val j = array(i)
        array(i) = array(length - 1 - i)
        array(length - 1 - i) = j
      })
    }

    if (reqSize > length) {
      val newArray = new Array[Byte](reqSize)
      System.arraycopy(array, 0, newArray, 0, length)
      return newArray
    }

    array
  }
}
