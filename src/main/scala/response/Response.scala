package response

sealed trait Response {
  def status: Int
}

object Response {
  case class Accepted(status: Int = 200, message: String) extends Response
  case class PhotoUrl(status: Int = 200, url: String) extends Response
  case class Error(status: Int = 500, message: String) extends Response
}
