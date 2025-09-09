package example

import cats.effect.{IO, IOApp}
import cats.implicits._
import io.circe.{Json, parser}
import org.http4s.ember.client.EmberClientBuilder
import io.circe.Decoder
import org.http4s.circe.CirceEntityDecoder._

object Weather extends IOApp.Simple {

  def run: IO[Unit] =
    EmberClientBuilder.default[IO].build.use { client =>
      def go: IO[Unit] =
        weatherFunc(client) >> getInput(
          "Do you want to check another city? (y/n): "
        ).flatMap(repeat => if (repeat.toLowerCase == "y") go else IO.unit)
      go
    }

  def printCities(indexedCities: List[(City, Int)]): IO[Unit] =
    indexedCities.traverse_ { case (city, i) =>
      IO.println(s"[$i] - ${city.EnglishName}")
    }

  def getInput(prompt: String): IO[String] =
    for {
      _ <- IO.print(prompt)
      line <- IO.readLine
    } yield line

  def fetchCities(client: org.http4s.client.Client[IO], citiesUri: String): IO[List[City]] = {
    client.expect[List[City]](citiesUri)
  }

  def getWeather(client: org.http4s.client.Client[IO], city: City): IO[List[WeatherModel]] = {
    val weatherURI: String =
          s"http://dataservice.accuweather.com/currentconditions/v1/${city.Key}?apikey=iUxLgSJkOao2GLy68sqFnV9G62sxMpEh"
    client.expect[List[WeatherModel]](weatherURI)
    
  }

  def weatherFunc(client: org.http4s.client.Client[IO]): IO[Unit] = {
    val citiesUri =
      "http://dataservice.accuweather.com/locations/v1/topcities/50?apikey=iUxLgSJkOao2GLy68sqFnV9G62sxMpEh"
    for {
      _             <- IO.println("Weather app starting...")
      cities        <- fetchCities(client, citiesUri)
      indexedCities = cities.zipWithIndex
      _             <- printCities(indexedCities)
      input         <- getInput("Enter the number for the city you wish to receive the weather report for: ")
      _             <- input.toIntOption match {
                        case Some(index) if indexedCities.isDefinedAt(index) =>
                          val (city, _) = indexedCities(index)
                          for {
                            weathers <- getWeather(client, city)
                            _ <- weathers.traverse_{ w =>
                                val time =
                                  if (w.IsDayTime) "Daytime"
                                  else "Night-time"
                                IO.println(s"${city.EnglishName}:\n${w.WeatherText}\n${w.Temperature.Metric.Value}C\n${time}")
                            }
                          } yield ()
                        case _ => IO.println("Invalid index")
      }
    } yield ()
  }
}

case class City(Key: String, EnglishName: String)
object City {
  implicit val decoder: Decoder[City] = io.circe.generic.semiauto.deriveDecoder
}

case class TemperatureMetric(Value: Double)
object TemperatureMetric {
  implicit val decoder: Decoder[TemperatureMetric] = io.circe.generic.semiauto.deriveDecoder
}
case class Temperature(Metric: TemperatureMetric)
object Temperature {
  implicit val decoder: Decoder[Temperature] = io.circe.generic.semiauto.deriveDecoder
}

case class WeatherModel(WeatherText: String, Temperature: Temperature, IsDayTime: Boolean)

object WeatherModel {
  implicit val decoder: Decoder[WeatherModel] = io.circe.generic.semiauto.deriveDecoder
}