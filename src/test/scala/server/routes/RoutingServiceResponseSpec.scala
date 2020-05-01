package server.routes

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import common.http.HttpRequests
import common.room.{RoomJsonSupport, RoomProperty, SharedRoom}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.core.RoomHandler
import server.matchmaking.MatchmakingHandler
import server.room.ServerRoom
import server.routing_service.RoutingService

import scala.concurrent.ExecutionContextExecutor

class RoutingServiceResponseSpec extends AnyFlatSpec with Matchers
  with ScalatestRouteTest
  with RoomJsonSupport
  with RouteCommonTestOptions
  with BeforeAndAfter {

  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var roomHandler = RoomHandler()
  private var routeService = RoutingService(roomHandler, MatchmakingHandler(roomHandler))
  private var route = routeService.route

  behavior of "Route Service routing with room handling"

  before {
    roomHandler = RoomHandler()
    routeService = RoutingService(roomHandler, MatchmakingHandler(roomHandler))
    route = routeService.route

    //define room type for test
    routeService.addRouteForRoomType(TestRoomType, () => ServerRoom())
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }


  it should "respond with a list of available rooms on GET request on path 'rooms'" in {
    getRoomsWithEmptyFilters ~> route ~> check {
      responseAs[Seq[SharedRoom]]
    }
  }


  it should "respond with a list of available rooms on GET request on path 'rooms/{type}' " in {
    getRoomsByTypeWithEmptyFilters ~> route ~> check {
      responseAs[Seq[SharedRoom]]
    }
  }


  it should "respond with a room that was created on POST request on path 'rooms/{type}' " in {
    postRoomWithEmptyProperties ~> route ~> check {
      responseAs[SharedRoom]
    }
  }

  it should "respond with an empty sequence if no rooms have been created " in {
    getRoomsByTypeWithEmptyFilters ~> route ~> check {
      responseAs[Seq[SharedRoom]] shouldBe empty
    }
  }

  it should "respond with all the rooms of the requested type on GET request on path 'rooms/{type}' " in {
    createRoomRequest(Set.empty)
    createRoomRequest(Set.empty)

    getRoomsByTypeWithEmptyFilters ~> route ~> check {
      responseAs[Seq[SharedRoom]] should have size 2
    }
  }

  private def createRoomRequest(testProperties: Set[RoomProperty] = Set.empty): SharedRoom = {
    HttpRequests.postRoom("")(TestRoomType, testProperties) ~> route ~> check {
      responseAs[SharedRoom]
    }
  }
}