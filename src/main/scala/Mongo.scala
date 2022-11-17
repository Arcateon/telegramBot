import com.mongodb.{MongoClientSettings, ServerApi, ServerApiVersion}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.model.Aggregates.sample
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.{ConnectionString, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object Mongo {

  //--------------------
  //
  // Connection to mongo
  //
  //--------------------

  private val mongoClientSettings = MongoClientSettings.builder()
    .applyConnectionString(ConnectionString("mongodb://localhost:27017"))
    .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
    .build()

  private val mongoClient = MongoClient(mongoClientSettings)
  private val dataBase: MongoDatabase = mongoClient.getDatabase("pidor")

  //---------------------
  //
  // Codecs
  //
  //---------------------

  case class PidorData(name: String, userId: String)
  case class PidorOfTheDay(name: String, userId: String, date: String)

  private val codecProvider: CodecProvider = Macros
    .createCodecProviderIgnoreNone[PidorData]()
  private val codecRegistry: CodecRegistry =
    fromRegistries(fromProviders(codecProvider)
      , DEFAULT_CODEC_REGISTRY)

  private val codecProviderPidorOfTheDay: CodecProvider = Macros
    .createCodecProviderIgnoreNone[PidorOfTheDay]()
  private val codecRegistryPidorOfTheDay: CodecRegistry =
    fromRegistries(fromProviders(codecProviderPidorOfTheDay)
      , DEFAULT_CODEC_REGISTRY)

  //---------------------
  //
  // Collections
  //
  //---------------------

  val users: MongoCollection[PidorData] = dataBase
    .withCodecRegistry(codecRegistry)
    .getCollection[PidorData]("users")

  val pidorOfTheDay: MongoCollection[PidorOfTheDay] = dataBase
    .withCodecRegistry(codecRegistryPidorOfTheDay)
    .getCollection[PidorOfTheDay]("pidorOfTheDay")

  //---------------------
  //
  // Methods
  //
  //---------------------

  def userRegistration(name: String, userId: String): Future[Boolean] = {

    val userDocument = users.find(and(equal("name", name),
      equal("userId", userId))).toFuture()

    userDocument flatMap { user =>
      if(user.isEmpty) {
        val newUser = PidorData(name, userId)
        users.insertOne(newUser).toFuture()
        Future.successful(true)
      } else Future.successful(false)
    }
  }

  def getNameOfPidorOfTheDay(date: String): Future[Seq[String]] = {

    val allPidors = pidorOfTheDay.find(equal("date", date)).toFuture()

    allPidors flatMap { doc =>
      if(doc.isEmpty) {
        val userDocument = users.aggregate(Seq(sample(1))).toFuture()
        userDocument flatMap { doc =>
          val res = doc map { user =>
            val insertDoc = PidorOfTheDay(user.name, user.userId, date)
            pidorOfTheDay.insertOne(insertDoc).toFuture()
            user.name
          }
          Future.successful(Seq(s"Выбор сделан! Пидор дня - ${res.mkString}"))
        }
      } else {
          allPidors flatMap { doc =>
            val res = doc map { user =>
              user.name
            }
            Future.successful(Seq(s"Пидор этого дня уже выбран! Это ${res.mkString}"))
        }
      }
    }

  }


}
