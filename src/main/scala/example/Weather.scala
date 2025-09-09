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

  def printWeather(weatherJson: String, city: City): IO[Unit] =
    parser.parse(weatherJson) match {
      case Right(json: Json) =>
        var weatherString: String = ""
        weatherString + s"${city.EnglishName}:\n"
        val temp_val: Either[io.circe.DecodingFailure, Double] =
          json.hcursor.downArray
            .downField("Temperature")
            .downField("Metric")
            .get[Double]("Value")
        val weather: Either[io.circe.DecodingFailure, String] =
          json.hcursor.downArray.get[String]("WeatherText")
        val daytime: Either[io.circe.DecodingFailure, Boolean] =
          json.hcursor.downArray.get[Boolean]("IsDayTime")
        temp_val match {
          case Right(value) =>
            weatherString = weatherString + s"Temperature: $value"
          case _ => null
        }
        weather match {
          case Right(value) =>
            weatherString = weatherString + s"\nWeather: $value"
          case _ => null
        }
        daytime match {
          case Right(true)  => weatherString = weatherString + s"\nDaytime"
          case Right(false) => weatherString = weatherString + s"\nNight time"
          case _            => null
        }
        IO.println(weatherString)

      case Left(error) => IO.println(s"Failed to parse JSON: $error")
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
                            _ <- weathers.traverse_(w =>
                                IO.println(s"${city.EnglishName}: ${w.WeatherText}, ${w.Temperature}C, daytime=${w.IsDayTime}")
                              )
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