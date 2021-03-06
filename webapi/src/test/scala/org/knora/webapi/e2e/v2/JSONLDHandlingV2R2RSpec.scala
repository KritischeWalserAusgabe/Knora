/*
 * Copyright © 2015-2019 the contributors (see Contributors.md).
 *
 * This file is part of Knora.
 *
 * Knora is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knora is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with Knora.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.knora.webapi.e2e.v2

import java.io.File
import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.http.javadsl.model.StatusCodes
import akka.http.scaladsl.testkit.RouteTestTimeout
import com.github.jsonldjava.utils.JsonUtils
import org.knora.webapi._
import org.knora.webapi.e2e.v2.ResponseCheckerR2RV2._
import org.knora.webapi.messages.store.triplestoremessages.RdfDataObject
import org.knora.webapi.routing.v2.ResourcesRouteV2
import org.knora.webapi.util.jsonld.JsonLDUtil
import org.knora.webapi.util.{FileUtil, JavaUtil}

import scala.concurrent.ExecutionContextExecutor

/**
  * End-to-end specification for the handling of JSONLD documents.
  */
class JSONLDHandlingV2R2RSpec extends R2RSpec {

    override def testConfigSource: String =
        """
          |# akka.loglevel = "DEBUG"
          |# akka.stdout-loglevel = "DEBUG"
        """.stripMargin

    private val resourcesPath = new ResourcesRouteV2(routeData).knoraApiPath

    implicit def default(implicit system: ActorSystem): RouteTestTimeout = RouteTestTimeout(settings.defaultTimeout)

    implicit val ec: ExecutionContextExecutor = system.dispatcher

    override lazy val rdfDataObjects: List[RdfDataObject] = List(
        RdfDataObject(path = "_test_data/all_data/anything-data.ttl", name = "http://www.knora.org/data/0001/anything"),
        RdfDataObject(path = "_test_data/demo_data/images-demo-data.ttl", name = "http://www.knora.org/data/00FF/images"),
        RdfDataObject(path = "_test_data/all_data/incunabula-data.ttl", name = "http://www.knora.org/data/0803/incunabula")
    )

    "The JSONLD processor" should {

        "expand prefixes (on the client side)" in {

            // JSONLD with prefixes and context object
            val jsonldWithPrefixes = FileUtil.readTextFile(new File("src/test/resources/test-data/resourcesR2RV2/NarrenschiffFirstPage.jsonld"))

            // expand JSONLD with JSONLD processor
            val jsonldParsedExpanded = JsonLDUtil.parseJsonLD(jsonldWithPrefixes)

            // expected result after expansion
            val expectedJsonldExpandedParsed = JsonLDUtil.parseJsonLD(FileUtil.readTextFile(new File("src/test/resources/test-data/resourcesR2RV2/NarrenschiffFirstPageExpanded.jsonld")))

            compareParsedJSONLDForResourcesResponse(expectedResponse = expectedJsonldExpandedParsed, receivedResponse = jsonldParsedExpanded)

        }

        "produce the expected JSONLD context object (on the server side)" in {

            Get("/v2/resources/" + URLEncoder.encode("http://rdfh.ch/0803/7bbb8e59b703", "UTF-8")) ~> resourcesPath ~> check {

                assert(status == StatusCodes.OK, response.toString)

                val receivedJSONLDAsScala: Map[IRI, Any] = JavaUtil.deepJavatoScala(JsonUtils.fromString(responseAs[String])).asInstanceOf[Map[IRI, Any]]

                val expectedJSONLDAsScala: Map[IRI, Any] = JavaUtil.deepJavatoScala(JsonUtils.fromString(FileUtil.readTextFile(new File("src/test/resources/test-data/resourcesR2RV2/NarrenschiffFirstPage.jsonld")))).asInstanceOf[Map[String, Any]]

                assert(receivedJSONLDAsScala("@context") == expectedJSONLDAsScala("@context"), "@context incorrect")

            }

        }

    }

}