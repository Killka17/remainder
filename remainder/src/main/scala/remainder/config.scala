package remainder

case object dbConfig {
  val dbName: String = System.getenv("POSTGRES_DB")
  val dbUser: String = System.getenv("POSTGRES_USER")
  val dbPassword: String = System.getenv("POSTGRES_PASSWORD")
  val dbHost: String = System.getenv("POSTGRES_HOST")
  val dbPort: String = System.getenv("POSTGRES_PORT")
}

case object serverConfig {
  val serverHost: String = System.getenv("SERVER_HOST")
  val serverPort: String = System.getenv("SERVER_PORT")
  val chatId: String = System.getenv("CHAT_ID")
  val telegramURL: String = System.getenv("TELEGRAM_URL")
}
