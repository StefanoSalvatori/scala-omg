package server.routes

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.testkit.TestKit
import akka.util.ByteString
import common.SharedRoom.Room
import common.{IntRoomPropertyValue, RoomJsonSupport, RoomProperty, Routes}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.RoomHandler
import server.room.ServerRoom
import server.route_service.RouteService

import scala.concurrent.ExecutionContextExecutor


class RouteServiceRoutesSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with RouteCommonTestOptions
  with BeforeAndAfter with RoomJsonSupport {

  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private val routeService = RouteService(RoomHandler())

  private val route = routeService.route


  behavior of "Route Service routing"

  before {
    //ensure to have at least one room-type
    routeService.addRouteForRoomType(TEST_ROOM_TYPE, ServerRoom(_))
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  it should " enable the addition of routes for new rooms type" in {
    Get(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }


  it should " reject requests if the given room type  does not exists" in {
    Get("/" + Routes.roomsByType("wrong-type")) ~> route ~> check {
      handled shouldBe false
    }
  }


  it should " reject requests if the given id does not exists" in {
    Get(ROOMS_WITH_TYPE + "/wrong-id") ~> route ~> check {
      handled shouldBe false
    }
  }

  /// --- Rooms routes ---

  /// GET rooms
  it should "handle GET request on path 'rooms' with room options as payload" in {
    makeRequestWithEmptyPayload(HttpMethods.GET)(ROOMS) ~> route ~> check {
      handled shouldBe true
    }
  }

  it should "handle GET request on path 'rooms' with no payload" in {
    Get(ROOMS) ~> route ~> check {
      handled shouldBe true
    }
  }


  /// GET rooms/{type}
  it should "handle GET request on path 'rooms/{type}' with room options as payload " in {
    makeRequestWithEmptyPayload(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }

  it should "handle GET request on path 'rooms/{type}' with no payload " in {
    Get(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }


  /// --- POST rooms/{type}
  it should "handle POST request on path 'rooms/{type}' with room options as payload" in {
    makeRequestWithEmptyPayload(HttpMethods.POST)(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }


  it should "handle POST request on path 'rooms/{type}' with no payload" in {
    Post(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }

  /*it should "create rooms with properties after a single POST request with payload" in {
    val testProperty = RoomProperty("A", IntRoomPropertyValue(1))
    val postReq = makePostWithProperties(Set(testProperty))
    println(postReq)
    postReq  ~> route ~> check {
      Get(ROOMS_WITH_TYPE) ~> route ~> check {
        responseAs[Seq[Room]] should have size 1
      }
    }
  }*/

  /*it should "create only one room after a single POST request" in {
   makePostWithProperties(Set(RoomProperty("A", IntRoomPropertyValue(1))))~> route ~> check {
      Get(ROOMS_WITH_TYPE) ~> route ~> check {
        responseAs[Seq[Room]] should have size 1
      }
    }
  }*/


  /// GET rooms/{type}/{id}
  it should "handle GET request on path 'rooms/{type}/{id}' if such id exists " in {

    val room = createRoom()
    Get("/" + Routes.roomByTypeAndId(TEST_ROOM_TYPE, room.roomId)) ~> route ~> check { //try to get the created room by id
      handled shouldBe true
    }
  }

  /// --- Web socket  ---
  it should "handle web socket request on path 'connection/{id}'" in {
    val room = createRoom()
    val wsClient = WSProbe()
    WS("/" + Routes.connectionRoute + "/" + room.roomId, wsClient.flow) ~> route ~>
      check {
        // check response for WS Upgrade headers
        isWebSocketUpgrade shouldEqual true

      }
  }


  /*
    /// PUT rooms/{type}
  it should "handle PUT request on path 'rooms/{type}' with room options as payload  " in {
    makeRequestWithEmptyFilter(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }

  it should "handle PUT request on path 'rooms/{type}' with no payload " in {
    Put(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }

   */


  private def createRoom(): Room = {
    Post(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Room]
    }
  }


}

