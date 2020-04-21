package server.matchmaking

import akka.actor.{Actor, Props}
import common.communication.CommunicationProtocol.ProtocolMessage
import common.room.Room.RoomType
import server.RoomHandler
import server.matchmaking.MatchmakingService.{JoinQueue, LeaveQueue, Matchmaker}
import server.room.Client
import common.communication.CommunicationProtocol.ProtocolMessageType._

object MatchmakingService {
  type Matchmaker = List[Client] => Option[Map[Client, Int]]

  trait MatchmakingRequest
  case class JoinQueue(client: Client) extends MatchmakingRequest
  case class LeaveQueue(client: Client) extends MatchmakingRequest


  def apply(matchmaker: Matchmaker, room: RoomType, roomHandler: RoomHandler): Props =
    Props(classOf[MatchmakingService], matchmaker, room, roomHandler)

}

/**
 *
 * @param matchmaker  the matchmaking strategy
 * @param roomType    teh type of room that will be created
 * @param roomHandler the room handler where to spawn the room
 */
class MatchmakingService(private val matchmaker: Matchmaker,
                         private val roomType: RoomType,
                         private val roomHandler: RoomHandler) extends Actor {

  var clients: Set[Client] = Set.empty

  override def receive: Receive = {
    case JoinQueue(client) =>
      this.clients = this.clients + client
      this.applyMatchmakingStrategy()

    case LeaveQueue(client) =>
      this.clients = this.clients - client

  }

  // apply the matchmaking strategy to the current list of clients. If the strategy can be applied, the room is
  // created and the clients are reomved from the quue
  private def applyMatchmakingStrategy(): Unit = {
    this.matchmaker(this.clients.toList).foreach(grouping => {
      val room = this.roomHandler.createRoom(roomType)
      grouping.keys.foreach(c => c.send(ProtocolMessage(MatchCreated, c.id, room.roomId)))
      this.clients = this.clients -- grouping.keys
    })
  }

}

