/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ly.stealth.kafka.metrics;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import kafka.consumer.*;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertNotNull;

public class MetricsReportingSteps {
    private final String zkConnect = "172.16.10.5:2181";
    private final String kafkaConnect = "172.16.10.10:9092";
    private final String topic = UUID.randomUUID().toString();
    private KafkaReporter kafkaReporter;
    private MetricRegistry registry;

    @Given("Kafka broker is up and 'metrics' topic is created.")
    public void startingKafkaReporterAndCon() {
        registry = new MetricRegistry();
        registry.counter("test_counter").inc();

        kafkaReporter = KafkaReporter.builder(registry,
                                              kafkaConnect,
                                              topic).build();
    }

    @When("KafkaReporter sends data to Kafka topic.")
    public void reporterWritesMetrics() {
        kafkaReporter.report();
    }

    @Then("Kafka consumer should be able to read this data.")
    public void consumerReadsMetrics() throws IOException {
        ConsumerConnector consumer = Consumer.createJavaConsumerConnector(createConsumerConfig());
        String message = readMessage(consumer);
        assertNotNull(message);
        ObjectMapper objectMapper = new ObjectMapper();
        KafkaMetricsReport report = objectMapper.readValue(message, KafkaMetricsReport.class);
        assertNotNull(report);
    }

    private ConsumerConfig createConsumerConfig() {
        Properties props = new Properties();
        props.put("zookeeper.connect", zkConnect);
        props.put("group.id", UUID.randomUUID().toString());
        props.put("auto.offset.reset", "smallest");
        props.put("zookeeper.session.timeout.ms", "30000");
        props.put("consumer.timeout.ms", "30000");
        return new ConsumerConfig(props);
    }

    public String readMessage(ConsumerConnector consumer) {
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, new Integer(1));
        KafkaStream<String, String> messageStream = consumer.createMessageStreamsByFilter(new Whitelist(topic),
                                                                                          1,
                                                                                          new StringDecoder(null),
                                                                                          new StringDecoder(null)).get(0);

        return messageStream.iterator().next().message();
    }
}
