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

/**
  * To be able to test UsersResponder, we need to be able to start UsersResponder isolated. Now the UsersResponder
  * extend ResponderADM which messes up testing, as we cannot inject the TestActor system.
  */
package org.knora.webapi.responders.admin

import java.util.UUID

import akka.actor.Status.Failure
import akka.testkit.{ImplicitSender, TestActorRef}
import com.typesafe.config.{Config, ConfigFactory}
import org.knora.webapi._
import org.knora.webapi.messages.admin.responder.groupsmessages._
import org.knora.webapi.messages.admin.responder.usersmessages.UserInformationTypeADM
import org.knora.webapi.util.MutableTestIri

import scala.concurrent.duration._


object GroupsResponderADMSpec {

    val config: Config = ConfigFactory.parseString(
        """
         akka.loglevel = "DEBUG"
         akka.stdout-loglevel = "DEBUG"
        """.stripMargin)
}

/**
  * This spec is used to test the messages received by the [[org.knora.webapi.responders.admin.UsersResponderADM]] actor.
  */
class GroupsResponderADMSpec extends CoreSpec(GroupsResponderADMSpec.config) with ImplicitSender {

    private val timeout = 5.seconds

    private val imagesProject = SharedTestDataADM.imagesProject
    private val imagesReviewerGroup = SharedTestDataADM.imagesReviewerGroup
    private val imagesProjectAdminGroup = SharedTestDataADM.imagesProjectAdminGroup
    private val imagesProjectMemberGroup = SharedTestDataADM.imagesProjectMemberGroup

    private val rootUser = SharedTestDataADM.rootUser

    "The GroupsResponder " when {

        "asked about all groups" should {
            "return a list" in {
                responderManager ! GroupsGetRequestADM(SharedTestDataADM.rootUser)
                val response = expectMsgType[GroupsGetResponseADM](timeout)
                // println(response.users)
                response.groups.nonEmpty should be (true)
                response.groups.size should be (2)
            }
        }

        "asked about a group identified by 'iri' " should {
            "return group info if the group is known " in {
                responderManager ! GroupGetRequestADM(imagesReviewerGroup.id, rootUser)
                expectMsg(GroupGetResponseADM(imagesReviewerGroup))
            }
            "return 'NotFoundException' when the group is unknown " in {
                responderManager ! GroupGetRequestADM("http://rdfh.ch/groups/notexisting", rootUser)

                expectMsgPF(timeout) {
                    case msg: akka.actor.Status.Failure => msg.cause.isInstanceOf[NotFoundException] should ===(true)
                }
            }
        }

        "used to modify group information" should {

            val newGroupIri = new MutableTestIri

            "CREATE the group and return the group's info if the supplied group name is unique" in {
                responderManager ! GroupCreateRequestADM(
                    CreateGroupApiRequestADM("NewGroup", Some("""NewGroupDescription with "quotes" and <html tag>"""), SharedTestDataADM.IMAGES_PROJECT_IRI, status = true, selfjoin = false),
                    SharedTestDataADM.imagesUser01,
                    UUID.randomUUID
                )

                val received: GroupOperationResponseADM = expectMsgType[GroupOperationResponseADM](timeout)
                val newGroupInfo = received.group

                newGroupInfo.name should equal ("NewGroup")
                newGroupInfo.description should equal ("""NewGroupDescription with "quotes" and <html tag>""")
                newGroupInfo.project should equal (imagesProject)
                newGroupInfo.status should equal (true)
                newGroupInfo.selfjoin should equal (false)

                // store for later usage
                newGroupIri.set(newGroupInfo.id)
            }

            "return a 'DuplicateValueException' if the supplied group name is not unique" in {
                responderManager ! GroupCreateRequestADM(
                    CreateGroupApiRequestADM("NewGroup", Some("NewGroupDescription"), SharedTestDataADM.IMAGES_PROJECT_IRI, status = true, selfjoin = false),
                    SharedTestDataADM.imagesUser01,
                    UUID.randomUUID
                )

                expectMsgPF(timeout) {
                    case msg: akka.actor.Status.Failure => msg.cause.isInstanceOf[DuplicateValueException] should ===(true)
                }
            }

            "return 'BadRequestException' if group name or project IRI are missing" in {

                /* missing group name */
                responderManager ! GroupCreateRequestADM(
                    CreateGroupApiRequestADM("", Some("NoNameGroupDescription"), SharedTestDataADM.IMAGES_PROJECT_IRI, status = true, selfjoin = false),
                    SharedTestDataADM.imagesUser01,
                    UUID.randomUUID
                )
                expectMsg(Failure(BadRequestException("Group name cannot be empty")))

                /* missing project */
                responderManager ! GroupCreateRequestADM(
                    CreateGroupApiRequestADM("OtherNewGroup", Some("OtherNewGroupDescription"), "", status = true, selfjoin = false),
                    SharedTestDataADM.imagesUser01,
                    UUID.randomUUID
                )
                expectMsg(Failure(BadRequestException("Project IRI cannot be empty")))
            }

            "UPDATE a group" in {
                responderManager ! GroupChangeRequestADM(
                    newGroupIri.get,
                    ChangeGroupApiRequestADM(Some("UpdatedGroupName"), Some("""UpdatedDescription with "quotes" and <html tag>""")),
                    SharedTestDataADM.imagesUser01,
                    UUID.randomUUID
                )

                val received: GroupOperationResponseADM = expectMsgType[GroupOperationResponseADM](timeout)
                val updatedGroupInfo = received.group

                updatedGroupInfo.name should equal ("UpdatedGroupName")
                updatedGroupInfo.description should equal ("""UpdatedDescription with "quotes" and <html tag>""")
                updatedGroupInfo.project should equal (imagesProject)
                updatedGroupInfo.status should equal (true)
                updatedGroupInfo.selfjoin should equal (false)
            }

            "return 'NotFound' if a not-existing group IRI is submitted during update" in {
                responderManager ! GroupChangeRequestADM(
                    groupIri = "http://rdfh.ch/groups/notexisting",
                    ChangeGroupApiRequestADM(Some("UpdatedGroupName"), Some("UpdatedDescription")),
                    SharedTestDataADM.imagesUser01,
                    UUID.randomUUID
                )

                expectMsgPF(timeout) {
                    case msg: akka.actor.Status.Failure => msg.cause.isInstanceOf[NotFoundException] should ===(true)
                }
            }

            "return 'BadRequest' if the new group name already exists inside the project" in {
                responderManager ! GroupChangeRequestADM(
                    newGroupIri.get,
                    ChangeGroupApiRequestADM(Some("Image reviewer"), Some("UpdatedDescription")),
                    SharedTestDataADM.imagesUser01,
                    UUID.randomUUID
                )

                expectMsgPF(timeout) {
                    case msg: akka.actor.Status.Failure => msg.cause.isInstanceOf[BadRequestException] should ===(true)
                }
            }

            "return 'BadRequest' if nothing would be changed during the update" in {

                an [BadRequestException] should be thrownBy ChangeGroupApiRequestADM(None, None, None, None)

                /*
                actorUnderTest ! GroupChangeRequestADM(
                    newGroupIri.get,
                    ChangeGroupApiRequestADM(None, None, None, None),
                    SharedAdminTestData.imagesUser01,
                    UUID.randomUUID
                )
                expectMsg(Failure(BadRequestException(s"No data sent in API request.")))
                */
            }

        }

        "used to query members" should {

            "return all members of a group identified by IRI" in {
                responderManager ! GroupMembersGetRequestADM(
                    groupIri = SharedTestDataADM.imagesReviewerGroup.id,
                    requestingUser = SharedTestDataADM.rootUser
                )

                val received: GroupMembersGetResponseADM = expectMsgType[GroupMembersGetResponseADM](timeout)

                received.members.map(_.id) should contain allElementsOf Seq(
                    SharedTestDataADM.multiuserUser.ofType(UserInformationTypeADM.RESTRICTED),
                    SharedTestDataADM.imagesReviewerUser.ofType(UserInformationTypeADM.RESTRICTED)
                ).map(_.id)
            }

            "return 'NotFound' when the group IRI is unknown" in {
                responderManager ! GroupMembersGetRequestADM(
                    groupIri = "http://rdfh.ch/groups/notexisting",
                    requestingUser = SharedTestDataADM.rootUser
                )

                expectMsgPF(timeout) {
                    case msg: akka.actor.Status.Failure => msg.cause.isInstanceOf[NotFoundException] should ===(true)
                }

            }
        }
    }

}
