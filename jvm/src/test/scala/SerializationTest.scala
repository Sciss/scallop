package org.rogach.scallop

import org.scalatest.FunSuite
import org.scalatest.Matchers
import java.io.{Serializable, ByteArrayOutputStream, ByteArrayInputStream, ObjectOutputStream, ObjectInputStream}

class TestConf(args: List[String]) extends ScallopConf(args) with Serializable {
  val apples = opt[Int]("apples")
  verify()

  protected final def writeReplace(): AnyRef = new TestConfSerializationProxy(this)
}
private class TestConfSerializationProxy(@transient private var orig: TestConf) extends Serializable {
  private def writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject()
    out.writeObject(orig.args.toArray)
  }
  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()
    orig = new TestConf(in.readObject().asInstanceOf[Array[String]].toList)
  }
  private def readResolve(): AnyRef = orig
}

class SerializationTest extends FunSuite with Matchers with UsefulMatchers {
  throwError.value = true

  test("simple ScallopConf serialization") {
    val conf = new TestConf(List("-a","3"))

    val bos = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(bos)
    out.writeObject(conf)
    out.flush()
    out.close();

    val bytes = bos.toByteArray()

    val bis = new ByteArrayInputStream(bytes);
    val in = new ObjectInputStream(bis);

    val conf2 = in.readObject().asInstanceOf[TestConf]
    conf2.apples() shouldEqual 3
  }
}
