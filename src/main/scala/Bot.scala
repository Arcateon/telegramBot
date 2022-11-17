import java.net.URLEncoder
import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.Implicits._
import com.bot4s.telegram.api.declarative._
import com.bot4s.telegram.api.{ChatActions, RequestHandler}
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models._

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}

object Bot extends TelegramBot
  with Polling
  with Commands[Future]
  with InlineQueries[Future]
  with ChatActions[Future] {

  override val client: RequestHandler[Future] = new ScalajHttpClient("TOKEN")

  onCommand("newpidor") { implicit msg =>
    val userId = msg.from.get.id.toString
    val userName = s"@${msg.from.get.username.mkString}"
    Mongo.userRegistration(userName, userId) flatMap { result =>
      if(result) reply("Теперь ты в игре!").void
      else reply("Ты уже участвуешь!").void
    }
  }

  onCommand("pidor") { implicit msg =>
    val date = java.time.LocalDate.now.toString
    val res = Await.result(Mongo.getNameOfPidorOfTheDay(date), 1.second)
    reply(res.mkString).void
  }

  def main(args: Array[String]): Unit = {
    val bot = Bot
    val eol = bot.run()
    println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
    scala.io.StdIn.readLine()
    bot.shutdown()
    Await.result(eol, Duration.Inf)

  }
}

