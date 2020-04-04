package server.examples

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import client.Client
import common.communication.{CommunicationProtocol, RoomProtocolSerializer}
import common.communication.CommunicationProtocol.RoomProtocolMessage
import server.GameServer

import scala.concurrent.{Await, Future}
import scala.io.StdIn

object TestWebSocket extends App {
  implicit val actorSystem: ActorSystem = ActorSystem()

  val HOST: String = "localhost"
  val PORT: Int = 8080
  val ESCAPE_TEXT = "quit"
  val ROOM_PATH = "chat"
  val gameServer: GameServer = GameServer(HOST, PORT)
  val client = Client(HOST, PORT)
  gameServer.defineRoom(ROOM_PATH, id => new ChatRoom(id))

  import scala.concurrent.duration._
  Await.ready(gameServer.start(), 10 seconds)
  val room = Await.result(client.createPublicRoom(ROOM_PATH, Set.empty), 10 seconds)

  val webSocketRequest = WebSocketRequest(s"ws://$HOST:$PORT/connection/${room.roomId}")
  val webSocketFlow = Http().webSocketClientFlow(webSocketRequest)
  val queue = Source.queue[Message](Int.MaxValue, OverflowStrategy.dropTail)
    .viaMat(webSocketFlow)(Keep.left)
    .toMat(Sink.foreach(msg => {
      val protocolReceived = RoomProtocolSerializer.parseFromSocket(msg)
      if (protocolReceived.isSuccess) {
        println(protocolReceived.get.payload)
      } else {
        println("Received malformed message")
      }
    }))(Keep.left)
    .run()

  this.joinRoom()
  //Start chat
  var msg = ""
  do {
    msg = StdIn.readLine("\n")
    this.sendToRoom(msg)
  } while (msg != ESCAPE_TEXT)

  Http().shutdownAllConnectionPools()
  Await.ready(gameServer.stop(), 10 seconds)
  Await.ready(gameServer.terminate(), 10 seconds)
  this.actorSystem.terminate()
  System.exit(0)

  private def sendToRoom(message: String): Unit = {
    this.queue.offer(RoomProtocolSerializer.writeToSocket(RoomProtocolMessage(CommunicationProtocol.MessageRoom, message)))

  }

  private def joinRoom() = {
    queue.offer(RoomProtocolSerializer.writeToSocket(RoomProtocolMessage(CommunicationProtocol.JoinRoom)))


  }


}



