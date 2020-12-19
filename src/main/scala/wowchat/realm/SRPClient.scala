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

import java.security.MessageDigest

/**
  * This class was taken from JaNGOS project and ported to Scala.
  * Look at JaNGOSAuth for further documentation about the algorithm used here
  *
  * https://github.com/Warkdev/JaNGOSAuth
  */
class SRPClient {

  var k: BigNumber = BigNumber(3)

  var a: BigNumber = BigNumber.rand(19)

  var A: BigNumber = _

  var x: BigNumber = _

  var M: BigNumber = _

  var S: BigNumber = _

  var K: BigNumber = _

  var md: MessageDigest = MessageDigest.getInstance("SHA1")

  def generateHashLogonProof: Array[Byte] = {
    md.update(A.asByteArray(32))
    md.update(M.asByteArray(20, false))
    md.update(K.asByteArray(40))

    md.digest()
  }

  def step1(account: Array[Byte], password: String,
            B: BigNumber, g: BigNumber, N: BigNumber, s: BigNumber): Unit = {
    val passwordUpper = password.toUpperCase

    A = g.modPow(a, N)

    md.update(A.asByteArray(32))
    md.update(B.asByteArray(32))

    val u = BigNumber(md.digest, true)

    val user = (account :+ ':'.toByte) ++ passwordUpper.getBytes("UTF-8")
    md.update(user)
    val p = md.digest

    md.update(s.asByteArray(32))
    md.update(p)

    val x = BigNumber(md.digest, true)

    S = B.-(g.modPow(x, N).*(k)).modPow(a.+(u.*(x)), N)

    val t = S.asByteArray(32)
    val t1 = new Array[Byte](16)
    val t2 = new Array[Byte](16)
    val vK = new Array[Byte](40)

    (0 until 16).foreach(i => {
      t1(i) = t(i * 2)
      t2(i) = t(i * 2 + 1)
    })

    md.update(t1)

    var digest = md.digest
    (0 until 20).foreach(i => {
      vK(i * 2) = digest(i)
    })

    md.update(t2)
    digest = md.digest
    (0 until 20).foreach(i => {
      vK(i * 2 + 1) = digest(i)
    })

    md.update(N.asByteArray(32))
    val hash = md.digest

    md.update(g.asByteArray(1))
    digest = md.digest
    (0 until 20).foreach(i => {
      hash(i) = (hash(i) ^ digest(i)).toByte
    })

    md.update(account)
    val t4 = md.digest

    K = BigNumber(vK, true)

    val t3 = BigNumber(hash, true)

    val t4_correct = BigNumber(t4, true)

    md.update(t3.asByteArray(20))
    md.update(t4_correct.asByteArray(20))
    md.update(s.asByteArray(32))
    md.update(A.asByteArray(32))
    md.update(B.asByteArray(32))
    md.update(K.asByteArray(40))

    M = BigNumber(md.digest)
  }

  override def toString: String = {
    "SRPClient(" +
      s"k=${k.toHexString}, " +
      s"A=${A.toHexString}, " +
      s"M=${M.toHexString}, " +
      s"S=${S.toHexString}, " +
      s"K=${K.toHexString}" +
      ")"
  }
}
