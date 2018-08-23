# SparkStream-DataPipeline

3 Data Pipeline approaches: 

a. com.myprojects.datapipeline.sparkstreaming.StreamingPipe
b. com.myprojects.datapipeline.sparkstreaming.StreamingPipe_withOutCheckpoint
c. com.myprojects.datapipeline.sparkstreaming.StreamingPipe_Hybrid


Used Confluent for Kafka:

Commands to start kafka broker, zookeeper and schema reistry: 

confluent start schema-registry 

Starting zookeeper
zookeeper is [UP]
Starting kafka
kafka is [UP]
Starting schema-registry
schema-registry is [UP]



Commands to create and delete the topic:

kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 10 --topic MetricData

kafka-topics --delete --zookeeper localhost:2181 --topic MetricData

Command to list topics

kafka-topics --list --zookeeper localhost:2181

Command to list the consumer group 
kafka-consumer-groups --zookeeper localhost:2181 --describe --group RG

Command to read/consumer using kafka utility 
sudo kafka-console-consumer --bootstrap-server localhost:9092 --topic MetricData --from-beginning


