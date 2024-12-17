case class config()

object config {
  val dbName = System.getenv("POSTGRES_DB")
  val dbUser = System.getenv("POSTGRES_USER")
  val dbPassword = System.getenv("POSTGRES_PASSWORD")
  val dbHost = System.getenv("POSTGRES_HOST")
  val serverHost = System.getenv("POSTGRES_HOST")
  val dbPort = System.getenv("POSTGRES_PORT")
  val chatId = System.getenv("CHAT_ID")
}
