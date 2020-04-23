package server.matchmaking

import java.util.UUID

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import common.TestConfig
import common.communication.CommunicationProtocol.{ProtocolMessage, ProtocolMessageType}
import common.room.Room.RoomId
import common.communication.CommunicationProtocol.{MatchmakeTicket, ProtocolMessage}
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.room.Room.RoomId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.matchmaking.MatchmakingService
import server.matchmaking.MatchmakingService.{JoinQueue, LeaveQueue, MatchmakingStrategy}
import server.RoomHandler
import server.matchmaking.MatchmakingService.{JoinQueue, LeaveQueue, Matchmaker}
import server.utils.{ExampleRooms, TestClient}


class MatchmakingServiceSpec extends TestKit(ActorSystem("ServerSystem", ConfigFactory.load()))
  with ImplicitSender
  with Matchers
  with AnyWordSpecLike
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  import akka.testkit.TestActorRef

  private var matchmakingServiceActor: TestActorRef[MatchmakingService] = _
  private var matchmakingServiceState: MatchmakingService = _
  private var roomHandler: RoomHandler = _
  private var client1: TestClient = _
  private var client2: TestClient = _

  //dummy matchmaking strategy that only pairs two clients
  private def matchmakingStrategy: MatchmakingStrategy =
    map => map.toList match {
      case c1 :: c2 :: _ => Some(Map(c1._1 -> 0, c2._1 -> 1))
      case _ => None
    }

  before {
    client1 = TestClient(UUID.randomUUID.toString)
    client2 = TestClient(UUID.randomUUID.toString)
    roomHandler = RoomHandler()
    roomHandler.defineRoomType(ExampleRooms.noPropertyRoomType, ExampleRooms.NoPropertyRoom)
    matchmakingServiceActor =
      TestActorRef(new MatchmakingService(matchmakingStrategy, ExampleRooms.noPropertyRoomType, roomHandler))
    matchmakingServiceState = matchmakingServiceActor.underlyingActor
  }

  after {
    matchmakingServiceActor ! PoisonPill
  }

  override def afterAll(): Unit = {
    system.terminate()
  }

  "A matchmaking service" should {
    "start with no clients in the queue" in {
      assert(matchmakingServiceState.clients.isEmpty)
    }

    "add clients to the matchmaking queue" in {
      matchmakingServiceActor ! JoinQueue(client1, None)
      assert(matchmakingServiceState.clients.size == 1)
    }

    "remove clients from the matchmaking queue" in {
      matchmakingServiceActor ! JoinQueue(client1, None)
      matchmakingServiceActor ! LeaveQueue(client1)
      assert(matchmakingServiceState.clients.isEmpty)
    }

    "not add the same client twice" in {
      matchmakingServiceActor ! JoinQueue(client1, None)
      matchmakingServiceActor ! JoinQueue(client1, None)
      assert(matchmakingServiceState.clients.size == 1)
    }

    "remove clients when they match the matchmaking rule" in {
      matchmakingServiceActor ! JoinQueue(client1, None)
      matchmakingServiceActor ! JoinQueue(client2, None)
      assert(matchmakingServiceState.clients.isEmpty)
    }

    "send clients a MatchCreated message with session id and roomId when they are matched with other players" in {
      matchmakingServiceActor ! JoinQueue(client1, None)
      matchmakingServiceActor ! JoinQueue(client2, None)
      assert(receivedMatchCreatedMessage(client1))
      assert(receivedMatchCreatedMessage(client2))
    }

    "create the room when some clients match the matchmaking strategy" in {
      matchmakingServiceActor ! JoinQueue(client1, None)
      matchmakingServiceActor ! JoinQueue(client2, None)
      assert(roomHandler.getAvailableRooms().nonEmpty)
    }
  }

  private def receivedMatchCreatedMessage(client: TestClient) = {
    client.allMessagesReceived.collect({
      case msg@ProtocolMessage(MatchCreated, id, ticket: MatchmakeTicket)
        if id == client.id && ticket.roomId.nonEmpty => msg
    }).nonEmpty

  }

}
