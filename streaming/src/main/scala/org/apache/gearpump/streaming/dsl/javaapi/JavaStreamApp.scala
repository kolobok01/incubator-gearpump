/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gearpump.streaming.dsl.javaapi

import java.util.Collection
import scala.collection.JavaConverters._

import org.apache.gearpump.cluster.UserConfig
import org.apache.gearpump.cluster.client.ClientContext
import org.apache.gearpump.streaming.dsl.{CollectionDataSource, StreamApp}
import org.apache.gearpump.streaming.source.DataSource

class JavaStreamApp(name: String, context: ClientContext, userConfig: UserConfig) {

  private val streamApp = StreamApp(name, context, userConfig)

  def source[T](collection: Collection[T], parallelism: Int,
      conf: UserConfig, description: String): JavaStream[T] = {
    val dataSource = new CollectionDataSource(collection.asScala.toSeq)
    source(dataSource, parallelism, conf, description)
  }

  def source[T](dataSource: DataSource, parallelism: Int,
      conf: UserConfig, description: String): JavaStream[T] = {
    new JavaStream[T](streamApp.source(dataSource, parallelism, conf, description))
  }

  def run(): Unit = {
    context.submit(streamApp)
  }
}
