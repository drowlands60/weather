package example

import cats.effect.{IO, IOApp}
import cats.implicits._
import io.circe.{Json, parser}
import org.http4s.ember.client.EmberClientBuilder

object Weather extends IOApp.Simple {

  def run: IO[Unit] =
    EmberClientBuilder.default[IO].build.use { client =>
      def go: IO[Unit] = 
        weatherFunc(client) >> getInput("Do you want to check another city? (y/n): ").flatMap( repeat => if (repeat.toLowerCase == "y") go else IO.unit )
      go
    }

  def printCities(indexedCities: Vector[(Json, Int)]): IO[Unit] =
    indexedCities.traverse_ { case (cityJson, i) =>
      cityJson.hcursor.get[String]("EnglishName") match {
        case Right(name) => IO.println(s"[$i] - $name")
        case Left(_)     => IO.println(s"[$i] - **Name Missing**")
      }
    }

  def getInput(prompt: String): IO[String] =
    for {
      _ <- IO.print(prompt)
      line <- IO.readLine
    } yield line

  def getWeather(client: org.http4s.client.Client[IO], city: Json): IO[String] =
    city.hcursor.get[String]("Key") match {
      case Left(error) =>
        IO.raiseError(new Exception(s"Failed to get location key: $error"))
      case Right(locationKey) =>
        val weatherURI: String =
          s"http://dataservice.accuweather.com/currentconditions/v1/$locationKey?apikey=iUxLgSJkOao2GLy68sqFnV9G62sxMpEh"
        client.expect[String](weatherURI)
    }
  def printWeather(weatherJson: String, city: Json): IO[Unit] =
    parser.parse(weatherJson) match {
      case Right(json: Json) =>
        var weatherString: String = ""
        city.hcursor.get[String]("EnglishName") match {
          case Right(value) => weatherString = weatherString + s"$value:\n"
          case _            => IO.unit
        }
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
          case _ => IO.unit
        }
        weather match {
          case Right(value) =>
            weatherString = weatherString + s"\nWeather: $value"
          case _ => IO.unit
        }
        daytime match {
          case Right(true)  => weatherString = weatherString + s"\nDaytime"
          case Right(false) => weatherString = weatherString + s"\nNight time"
          case _            => IO.unit
        }
        IO.println(weatherString)

      case Left(error) => IO.println(s"Failed to parse JSON: $error")
    }

  def weatherFunc(client: org.http4s.client.Client[IO]): IO[Unit] = {
    val uri =
      "http://dataservice.accuweather.com/locations/v1/topcities/50?apikey=iUxLgSJkOao2GLy68sqFnV9G62sxMpEh"
    for {
      _ <- IO.println("Weather app starting...")
      body <- client.expect[String](uri)
      _ <- parser.parse(body) match {
        case Right(response: Json) =>
          val cities: Vector[Json] = response.asArray.getOrElse(Vector.empty)
          val indexedCities: Vector[(Json, Int)] = cities.zipWithIndex

          for {
            _ <- printCities(indexedCities)

            input <- getInput(
              "Enter the number for the city you wish to receive the weather report for: "
            )
            cityIndexOpt = input.toIntOption

            _ <- cityIndexOpt match {
              case Some(index) =>
                indexedCities.collectFirst {
                  case (json, i) if i == index => json
                } match {
                  case Some(cityJson) =>
                    getWeather(client, cityJson).flatMap(weatherBody => printWeather(weatherBody, cityJson))
                  case None => IO.println("No city found at that index.")
                }
              case None =>
                IO.println("Invalid number entered.")
            }
          } yield ()
        case Left(error) => IO.println(s"Failed to parse JSON: $error")
      }
    } yield ()
  }

}
