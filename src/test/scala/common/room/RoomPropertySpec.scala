package common.room

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import common.room.RoomPropertyValueConversions._

class RoomPropertySpec extends AnyFlatSpec
    with Matchers
    with BeforeAndAfter {

  val intValue = 0; val intPropertyValue: IntRoomPropertyValue = intValue
  val stringValue = "abc"; val stringPropertyValue: StringRoomPropertyValue = stringValue
  val booleanValue = false; val booleanPropertyValue: BooleanRoomPropertyValue = booleanValue
  val doubleValue = 0.1; val doublePropertyValue: DoubleRoomPropertyValue = doubleValue

  behavior of "Room property values"

  it should "return correct values" in {
    intPropertyValue.value shouldEqual intValue
    stringPropertyValue.value shouldEqual stringValue
    booleanPropertyValue.value shouldEqual booleanValue
    doublePropertyValue.value shouldEqual doubleValue
  }

  it should "correctly transform a property value in the corresponding first class value" in {
    RoomPropertyValue valueOf intPropertyValue shouldEqual intValue
    RoomPropertyValue valueOf stringPropertyValue shouldEqual stringValue
    RoomPropertyValue valueOf booleanPropertyValue shouldEqual booleanValue
    RoomPropertyValue valueOf doublePropertyValue shouldEqual doubleValue
  }

  it should "instantiate the correct property value, starting from an unknown type" in {
    val intValue = 1; val intTest: Any = intValue
    RoomPropertyValue propertyValueFrom intTest shouldEqual IntRoomPropertyValue(intValue)
    val stringValue = "abc"; val stringTest: Any = stringValue
    RoomPropertyValue propertyValueFrom stringTest shouldEqual StringRoomPropertyValue(stringValue)
    val booleanValue = true; val booleanTest: Any = booleanValue
    RoomPropertyValue propertyValueFrom booleanTest shouldEqual BooleanRoomPropertyValue(booleanValue)
    val doubleValue = 0.1; val doubleTest: Any = doubleValue
    RoomPropertyValue propertyValueFrom doubleTest shouldEqual DoubleRoomPropertyValue(doubleValue)
  }
}
