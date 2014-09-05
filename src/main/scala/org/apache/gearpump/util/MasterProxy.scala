/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gearpump.util

import akka.actor._
import org.apache.gearpump.cluster.Configs
import org.apache.gearpump.transport.HostPort

class MasterProxy(masters: Iterable[HostPort])
  extends Actor with Stash with ActorLogging {

  val contacts = masters.map { master =>
    s"akka.tcp://${Configs.MASTER}@${master.host}:${master.port}/user/${Configs.MASTER_PROXY}"
  }.map { url =>
    context.actorSelection(url)
  }

  contacts foreach { _ ! Identify(None) }

  override def postStop(): Unit = {
    super.postStop()
  }

  def receive = establishing

  def establishing: Actor.Receive = {
    case ActorIdentity(_, Some(receptionist)) =>
      context watch receptionist
      log.info("Connected to [{}]", receptionist.path)
      context.watch(receptionist)
      unstashAll()
      context.become(active(receptionist))
    case ActorIdentity(_, None) => // ok, use another instead
    case msg => stash()
  }

  def active(receptionist: ActorRef): Actor.Receive = {
    case Terminated(receptionist) ⇒
      log.info("Lost contact with [{}], restablishing connection", receptionist)
      contacts foreach { _ ! Identify(None) }
      context.become(establishing)
    case _: ActorIdentity ⇒ // ok, from previous establish, already handled
    case msg => receptionist forward msg
  }
}