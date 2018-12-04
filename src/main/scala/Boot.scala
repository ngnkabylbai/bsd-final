import java.time.Duration

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.javadsl.StreamConverters
import akka.util.Timeout
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{CreateBucketRequest, GetBucketLocationRequest}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import response.Response
import service.PhotoService

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.io.StdIn


object Boot extends App with JsonSupport {
  implicit val system: ActorSystem = ActorSystem("final-exam-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // needed fot akka's ASK pattern
  implicit val timeout: Timeout = Timeout(60.seconds)

  val bucketName = "nkabylbay-final-user-bucket"

  // amazon credentials
  val awsCreds = new BasicAWSCredentials("AKIAIRZ5IB6V53A6FFUQ", "b/9kQZ00SJ5ngZHV95lJfpawUFtQYPsQySb2wIgu")

  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
    .withRegion(Regions.EU_WEST_1)
    .build()

  if (!s3Client.doesBucketExistV2(bucketName)) {
    println("Bucket does't exist. Creating...")
    s3Client.createBucket(new CreateBucketRequest(bucketName))
    val bucketLocation = s3Client.getBucketLocation(new GetBucketLocationRequest(bucketName))
    println(s"Bucket $bucketLocation created successfully")
  }

  val photoService: ActorRef = system.actorOf(PhotoService.props(s3Client, bucketName))

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
          concat(
            delete {
              complete(
                (photoService ? PhotoService.DeletePhoto(userId, fileName)).mapTo[Either[Response.Error, Response.Accepted]]
              )
            },
            get {
              complete(
                (photoService ? PhotoService.GetPhoto(userId, fileName)).mapTo[Either[Response.Error, Response.Accepted]]
              )
            }
          )
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