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

package org.knora.webapi

import akka.actor._
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.knora.webapi.app._
import org.knora.webapi.http.CORSSupport.CORS
import org.knora.webapi.http.ServerVersion.addServerHeader
import org.knora.webapi.messages.app.appmessages._
import org.knora.webapi.responders._
import org.knora.webapi.routing.admin._
import org.knora.webapi.routing.v1._
import org.knora.webapi.routing.v2._
import org.knora.webapi.routing._
import org.knora.webapi.store._
import org.knora.webapi.util.{CacheUtil, StringFormatter}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.languageFeature.postfixOps
import scala.util.{Failure, Success}

/**
  * Knora Core abstraction.
  */
trait Core {
    implicit val system: ActorSystem

    implicit val settings: SettingsImpl

    implicit val materializer: ActorMaterializer

    implicit val executionContext: ExecutionContext
}

/**
  * The applications actor system.
  */
trait LiveCore extends Core {

    /**
      * The application's actor system.
      */
    implicit lazy val system: ActorSystem = ActorSystem("webapi")

    /**
      * The application's configuration.
      */
    implicit lazy val settings: SettingsImpl = Settings(system)

    /**
      * Provides the actor materializer (akka-http)
      */
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    /**
      * Provides the default global execution context
      */
    implicit val executionContext: ExecutionContext = system.dispatchers.lookup(KnoraDispatchers.KnoraBlockingDispatcher)
}

/**
  * Provides methods for starting and stopping Knora from within another application. This is where the actor system
  * along with the three main supervisor actors is started. All further actors are started and supervised by those
  * three actors.
  */
trait KnoraService {
    this: Core =>

    // Initialise StringFormatter with the system settings. This must happen before any responders are constructed.
    StringFormatter.init(settings)

    import scala.language.postfixOps

    /**
      * The actor used at startup, transitioning between states, and storing the application application wide variables in a thread safe manner.
      */
    protected val applicationStateActor: ActorRef = system.actorOf(Props(new ApplicationStateActor).withDispatcher(KnoraDispatchers.KnoraActorDispatcher), name = APPLICATION_STATE_ACTOR_NAME)


    // #supervisors
    /**
      * The supervisor actor that forwards messages to actors that deal with persistent storage.
      */
    protected val storeManager: ActorRef = system.actorOf(Props(new StoreManager with LiveActorMaker).withDispatcher(KnoraDispatchers.KnoraActorDispatcher), name = StoreManagerActorName)


    /**
      * The supervisor actor that forwards messages to responder actors to handle API requests.
      */
    protected val responderManager: ActorRef = system.actorOf(Props(new ResponderManager(applicationStateActor, storeManager) with LiveActorMaker).withDispatcher(KnoraDispatchers.KnoraActorDispatcher), name = RESPONDER_MANAGER_ACTOR_NAME)
    // #supervisors

    /**
      * Timeout definition
      */
    implicit protected val timeout: Timeout = settings.defaultTimeout

    /**
      * A user representing the Knora API server, used for initialisation on startup.
      */
    private val systemUser = KnoraSystemInstances.Users.SystemUser

    /**
      * Provide logging
      */
    protected lazy val log: LoggingAdapter = akka.event.Logging(system, this.getClass.getName)

    /**
      * Route data.
      */
    private val routeData = KnoraRouteData(system, applicationStateActor, responderManager, storeManager)


    /**
      * All routes composed together and CORS activated.
      */
    private val apiRoutes: Route = addServerHeader(CORS(
        new HealthRoute(routeData).knoraApiPath ~
        new RejectingRoute(routeData).knoraApiPath ~
        new ResourcesRouteV1(routeData).knoraApiPath ~
        new ValuesRouteV1(routeData).knoraApiPath ~
        new StandoffRouteV1(routeData).knoraApiPath ~
        new ListsRouteV1(routeData).knoraApiPath ~
        new ResourceTypesRouteV1(routeData).knoraApiPath ~
        new SearchRouteV1(routeData).knoraApiPath ~
        new AuthenticationRouteV1(routeData).knoraApiPath ~
        new AssetsRouteV1(routeData).knoraApiPath ~
        new CkanRouteV1(routeData).knoraApiPath ~
        new UsersRouteV1(routeData).knoraApiPath ~
        new ProjectsRouteV1(routeData).knoraApiPath ~
        new OntologiesRouteV2(routeData).knoraApiPath ~
        new SearchRouteV2(routeData).knoraApiPath ~
        new ResourcesRouteV2(routeData).knoraApiPath ~
        new ValuesRouteV2(routeData).knoraApiPath ~
        new StandoffRouteV2(routeData).knoraApiPath ~
        new ListsRouteV2(routeData).knoraApiPath ~
        new AuthenticationRouteV2(routeData).knoraApiPath ~
        new GroupsRouteADM(routeData).knoraApiPath ~
        new ListsRouteADM(routeData).knoraApiPath ~
        new PermissionsRouteADM(routeData).knoraApiPath ~
        new ProjectsRouteADM(routeData).knoraApiPath ~
        new StoreRouteADM(routeData).knoraApiPath ~
        new UsersRouteADM(routeData).knoraApiPath ~
        new SipiRouteADM(routeData).knoraApiPath ~
        new SwaggerApiDocsRoute(routeData).knoraApiPath,
        settings,
        log
    ))

    // #startService
    /**
      * Starts the Knora API server.
      */
    def startService(skipLoadingOfOntologies: Boolean): Unit = {

        val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(Route.handlerFlow(apiRoutes), settings.internalKnoraApiHost, settings.internalKnoraApiPort)

        bindingFuture onComplete {
            case Success(_) => {

                // start monitoring reporters
                startReporters()

                // Kick of startup procedure.
                applicationStateActor ! InitStartUp(skipLoadingOfOntologies)
            }
            case Failure(ex) => {
                log.error("Failed to bind to {}:{}! - {}", settings.internalKnoraApiHost, settings.internalKnoraApiPort, ex.getMessage)
                stopService()
            }
        }
    }
    // #startService

    /**
      * Stops Knora.
      */
    def stopService(): Unit = {
        log.info("KnoraService - Shutting down.")
        Http().shutdownAllConnectionPools()
        CacheUtil.removeAllCaches()
        // Kamon.stopAllReporters()
        system.terminate()
        Await.result(system.whenTerminated, 30 seconds)
    }

    /**
      * Start the different reporters if defined. Reporters are the connection points between kamon (the collector) and
      * the application which we will use to look at the collected data.
      */
    private def startReporters(): Unit = {

        /*
        val prometheusReporter = Await.result(applicationStateActor ? GetPrometheusReporterState(), 1.second).asInstanceOf[Boolean]
        if (prometheusReporter) {
            Kamon.addReporter(new PrometheusReporter()) // metrics
        }

        val zipkinReporter = Await.result(applicationStateActor ? GetZipkinReporterState(), 1.second).asInstanceOf[Boolean]
        if (zipkinReporter) {
            Kamon.addReporter(new ZipkinReporter()) // tracing
        }

        val jaegerReporter = Await.result(applicationStateActor ? GetJaegerReporterState(), 1.second).asInstanceOf[Boolean]
        if (jaegerReporter) {
            Kamon.addReporter(new JaegerReporter()) // tracing
        }
        */
    }
}
