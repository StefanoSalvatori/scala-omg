package common.communication

import java.text.ParseException

import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.{ByteString, ByteStringBuilder}
import common.communication.CommunicationProtocol.{ProtocolMessage, ProtocolMessageSerializer}
import org.apache.commons.lang3.SerializationUtils

import scala.concurrent.Future

/**
 * A SocketSerializer for [[common.communication.CommunicationProtocol.ProtocolMessage]] that can write and read
 * them as binary objects. The payload is serialized according to the java.io.Serialization methods
 */
case class BinaryProtocolSerializer(implicit val materializer: Materializer) extends ProtocolMessageSerializer {

  import scala.concurrent.duration._

  private implicit val executor = materializer.executionContext

  override def parseFromSocket(msg: Message): Future[ProtocolMessage] = msg match {
    case BinaryMessage.Strict(binaryMessage) => parseBinaryMessage(binaryMessage)

    // ignore binary messages but drain content to avoid the stream being clogged
    case BinaryMessage.Streamed(binaryStream) => binaryStream
      .completionTimeout(5 seconds)
      .runFold(new ByteStringBuilder())((b, e) => b.append(e))
      .map(b => b.result)
      .flatMap(binary => parseBinaryMessage(BinaryMessage.Strict(binary).data))

    case _ => Future.failed(new ParseException(msg.toString, -1))
  }

  override def prepareToSocket(msg: ProtocolMessage): BinaryMessage =
    BinaryMessage.Strict(ByteString(SerializationUtils.serialize(msg)))


  private def parseIgnoreStream(bm: BinaryMessage.Streamed): Unit = {
    bm.dataStream.runWith(Sink.ignore)
  }

  private def parseBinaryMessage(msg: ByteString): Future[ProtocolMessage] = {
    try {
      Future.successful(SerializationUtils.deserialize(msg.toArray).asInstanceOf[ProtocolMessage])
    } catch {
      case _: Exception => Future.failed(new ParseException(msg.toString, -1))
    }
  }
}
