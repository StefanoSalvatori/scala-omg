package server.matchmaking

import akka.actor.{Actor, Props}
import common.communication.CommunicationProtocol.ProtocolMessage
import common.room.Room.RoomType
import server.RoomHandler
import server.matchmaking.MatchmakingService.{JoinQueue, LeaveQueue, MatchmakingStrategy}
import server.room.Client
import common.communication.CommunicationProtocol.ProtocolMessageType._

object MatchmakingService {
  type MatchmakingStrategy = Map[Client, Any] => Option[Map[Client, Int]]

  trait MatchmakingRequest
  case class JoinQueue(client: Client, clientInfo: Any) extends MatchmakingRequest
  case class LeaveQueue(client: Client) extends MatchmakingRequest


  def apply(matchmaker: MatchmakingStrategy, room: RoomType, roomHandler: RoomHandler): Props =
    Props(classOf[MatchmakingService], matchmaker, room, roomHandler)

}

/**
 *
 * @param matchmakingStrategy  the matchmaking strategy
 * @param roomType    teh type of room that will be created
 * @param roomHandler the room handler where to spawn the room
 */
class MatchmakingService(private val matchmakingStrategy: MatchmakingStrategy,
                         private val roomType: RoomType,
                         private val roomHandler: RoomHandler) extends Actor {

  var clients: Map[Client, Any] = Map.empty

  override def receive: Receive = {
    case JoinQueue(client, info) =>
      this.clients = this.clients + (client -> info)
      this.applyMatchmakingStrategy()

    case LeaveQueue(client) =>
      this.clients = this.clients - client

  }

  // apply the matchmaking strategy to the current list of clients. If the strategy can be applied, the room is
  // created and the clients are removed from the queue
  private def applyMatchmakingStrategy(): Unit = {
    this.matchmakingStrategy(this.clients).foreach(grouping => {
      val room = this.roomHandler.createRoom(roomType)
      grouping.keys.foreach(c => c.send(ProtocolMessage(MatchCreated, c.id, room.roomId)))
      this.clients = this.clients -- grouping.keys
    })
  }

}
