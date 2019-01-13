package wowchat.common

import scala.collection.mutable

object LRUMap {

  def empty[K, V](): mutable.Map[K, V] = empty(10000)
  def empty[K, V](maxSize: Int): mutable.Map[K, V] = new LRUMap[K, V](maxSize)
}

class LRUMap[K, V](maxSize: Int) extends mutable.LinkedHashMap[K, V] {

  override def get(key: K): Option[V] = {
    val ret = remove(key)
    if (ret.isDefined) {
      super.put(key, ret.get)
    }
    ret
  }

  override def put(key: K, value: V): Option[V] = {
    while (size >= maxSize) {
      remove(firstEntry.key)
    }
    super.put(key, value)
  }
}
