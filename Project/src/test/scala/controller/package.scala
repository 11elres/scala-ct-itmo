import io.circe.Json
import io.circe.parser.parse
import org.scalatest.matchers._
import sttp.client3.Response

package object controller {

  def beTheSameJson(expected: Json): Matcher[Response[Either[String, String]]] =
    new Matcher[Response[Either[String, String]]] {
      def apply(response: Response[Either[String, String]]): MatchResult = {
        val answerJson = parse(response.body.fold(identity, identity))
        val isParsed   = answerJson.isRight

        MatchResult(
          isParsed && answerJson.toOption.get == expected,
          if (!isParsed)
            s"incorrect json in response: ${response.body}"
          else
            s"got not expected json: ${answerJson.toOption.get.noSpaces}, expected: ${expected.noSpaces}",
          "json is like expected"
        )
      }
    }

}
