/*
 * Copyright (C) 2016 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.streaming.kafka010

import java.util.UUID

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
private[spark] class DirectKafkaStreamIT extends TemporalDataSuite {

  override val kafkaTopic = s"$DefaultKafkaTopic-${this.getClass().getName()}-${UUID.randomUUID().toString}"

  test("Kafka Receiver should read all the records") {

    val kafkaStream = KafkaUtils.createDirectStream[String, String](
      ssc,
      preferredHosts,
      ConsumerStrategies.Subscribe[String, String](List(kafkaTopic), getSparkKafkaParams))
    val totalEvents = ssc.sparkContext.accumulator(0L, "Number of events received")

    // Fires each time the configured window has passed.
    kafkaStream.foreachRDD(rdd => {
      if (!rdd.isEmpty()) {
        val count = rdd.count()
        // Do something with this message
        log.info(s"EVENTS COUNT : \t $count")
        totalEvents += count
        //rdd.collect().sortBy(event => event.toInt).foreach(event => print(s"$event, "))
      } else log.info("RDD is empty")
      log.info(s"TOTAL EVENTS : \t $totalEvents")
    })

    log.info(s"Starting Spark Streaming with options: \n $getSparkKafkaParams ...")
    // Start the computation
    ssc.start()
    log.info(s"Spark Streaming started correctly")

    log.info(s"Sending $totalRegisters messages to kafka ... ")
    //Send registers to Kafka
    val producer = getProducer(mandatoryOptions ++ Map("bootstrap.servers" -> kafkaHosts))
    for (register <- 1 to totalRegisters) {
      send(producer, kafkaTopic, register.toString)
    }
    close(producer)
    log.info(s"Inserted $totalRegisters in kafka correctly")

    // Wait for the computation to terminate
    ssc.awaitTerminationOrTimeout(10000L)

    assert(totalEvents.value === totalRegisters.toLong)
  }
}

