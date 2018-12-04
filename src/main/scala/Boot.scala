import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.stream.javadsl.StreamConverters
import akka.util.Timeout
import response.Response
import service.PhotoService

import scala.concurrent.duration._
import java.time.Duration

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{CreateBucketRequest, GetBucketLocationRequest}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn


object Boot extends App with JsonSupport {
  implicit val system: ActorSystem             = ActorSystem("final-exam-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // needed fot akka's ASK pattern
  implicit val timeout: Timeout = Timeout(60.seconds)

  val bucketName = "final-user-bucket-1234"

  // amazon credentials
  val awsCreds = new BasicAWSCredentials("your_access_key", "your_secret_key")

  val s3Client: AmazonS3 = ???

  val photoService: ActorRef = ???

  val route: Route = {
    pathPrefix("photo" / Segment) { userId =>
      concat(
        post {

          // ex: POST localhost:8081/photo/user-12
          fileUpload("photoUpload") {
            case (fileInfo, fileStream) =>

              // Photo upload
              // fileInfo -- information about file, including FILENAME
              // filestream -- stream data of file

              val inputStream = fileStream.runWith(StreamConverters.asInputStream(Duration.ofSeconds(5)))
              complete {
                (photoService ? PhotoService.UploadPhoto(inputStream, userId, fileInfo.fileName)).mapTo[Either[Response.Error, Response.Accepted]]
              }
          }
        },
        path(Segment) { fileName =>
          // TODO: implement GET method
          // ex: GET localhost:8081/photo/user-12/2.png

          // TODO: implement DELETE method
          // ex: DELETE localhost:8081/photo/user-12/6.png
        }
      )
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8081)
  println(s"Server online at http://localhost:8081/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.terminate()) // and shutdown when done
}