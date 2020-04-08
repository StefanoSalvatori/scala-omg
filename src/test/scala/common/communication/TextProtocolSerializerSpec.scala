package common.communication

import java.text.ParseException
import java.util.UUID

import akka.http.scaladsl.model.ws.TextMessage
import common.communication.CommunicationProtocol._
import org.scalatest.flatspec.AnyFlatSpec
import ProtocolMessageType._

class TextProtocolSerializerSpec extends AnyFlatSpec {

  private val serializer: TextProtocolSerializer.type = TextProtocolSerializer
  private val separator = serializer.SEPARATOR


  behavior of "Room Protocol Text Serializer"

  it should "assign unique string codes to protocol message types" in {
    val messageTypes = ProtocolMessageType.values.toList
    val stringCodes = messageTypes.map(t => serializer.prepareToSocket(RoomProtocolMessage(t)))
    assert(stringCodes.size == stringCodes.toSet.size)
  }


  it should s"write messages to sockets in the format 'code{separator}sessionId{separator}payload'" in {
    val sessionId = UUID.randomUUID.toString
    val messageToSend = RoomProtocolMessage(MessageRoom, sessionId, "Hello")
    val written = serializer.prepareToSocket(messageToSend)
    val expected = TextMessage.Strict(
      MessageRoom.id.toString + separator + sessionId + separator + "Hello")
    assert(written == expected)

  }

  it should "correctly parse text messages with no payload and no sessionId received from a socket" in {
    val joinOkCode = JoinOk.id.toString
    val messageToReceive = TextMessage.Strict(s"$joinOkCode$separator$separator")
    val expected = RoomProtocolMessage(JoinOk)
    assert(serializer.parseFromSocket(messageToReceive).get == expected)

  }

  it should "correctly parse text messages with payload and sessionId received from a socket" in {
    val leaveRoomCode = LeaveRoom.id.toString
    val sessionId = UUID.randomUUID.toString
    val messageToReceive =
      TextMessage.Strict(s"$leaveRoomCode$separator$sessionId${separator}Payload")
    val expected = RoomProtocolMessage(LeaveRoom, sessionId, "Payload")
    assert(serializer.parseFromSocket(messageToReceive).get == expected)
  }

  it should "fail to parse malformed messages" in {
    val messageToReceive = TextMessage.Strict("foo")
    val parseResult = serializer.parseFromSocket(messageToReceive)
    assert(parseResult.isFailure)
    assertThrows[ParseException] {
      parseResult.get
    }
  }

  it should "fail with NoSuchElementException parsing messages with an unknown type" in {
    val messageToReceive = TextMessage.Strict(s"97${separator}id${separator}Payload")
    val parseResult = serializer.parseFromSocket(messageToReceive)
    assert(parseResult.isFailure)
    assertThrows[NoSuchElementException] {
      parseResult.get
    }
  }

}