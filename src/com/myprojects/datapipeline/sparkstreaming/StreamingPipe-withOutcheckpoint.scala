package com.myprojects.datapipeline.sparkstreaming

import org.apache.spark.streaming.kafka._
import kafka.serializer.StringDecoder
import kafka.message.MessageAndMetadata
import kafka.utils.ZkUtils
import kafka.utils.ZKGroupTopicDirs


import kafka.utils.ZKStringSerializer$

import kafka.common.TopicAndPartition
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.log4j.Logger

import org.apache.kafka.common.serialization.StringDeserializer 
import java.util.concurrent.atomic.LongAccumulator

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.zookeeper.KeeperException;



object StreamingPipe_withOutCheckpoint {
  
   val sparkConf = new SparkConf().setAppName("StreamingPipeLine").setMaster("local[2]")
   LogHelper.log.debug("test") 
   val zookeeperHosts = "localhost:2181"; // If multiple zookeeper then -> String zookeeperHosts = "192.168.20.1:2181,192.168.20.2:2181";
   val topics = "MetricData";
   val consumer_group = "my_group"
   val brokers = "localhost:9092,localhost:9093"
   val zkGroupTopicDirs = new ZKGroupTopicDirs(consumer_group ,topics)  // Assuming that streaming reads from one single topic. If there are multiple topics, make code changes accordingly.. 
   val sessionTimeOutInMs = 15 * 1000; // 15 secs
   val connectionTimeOutInMs = 10 * 1000; // 10 secs
    // Create direct kafka stream with brokers and topics
   val topicsSet = topics.split(",").toSet
        System.out.println(topicsSet);
    
   val kafkaParams = Map[String, String](
   "bootstrap.servers" -> "localhost:9092,localhost:9093",
   "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
   "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
//   "auto.offset.reset" -> "largest",
   "group.id" -> consumer_group
   )                               
   
   def processData(dsStream: InputDStream[(String, String)]) {
     dsStream.foreachRDD(rdd => 
      {
        LogHelper.log.debug("Rdd : with partition size : " + rdd.partitions.size + " message count in the batch: " + rdd.count); 
        println("Rdd : with partition" + rdd.partitions.size + " count: " + rdd.count) 

        // process the rdd and persist the offset into zookeeper( if you are not using spark check pointing)
        
        val offsetsRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        peristOffset(offsetsRanges)
        
        val offsetsRangesStr = offsetsRanges.map(offsetRange => s"${offsetRange.partition}:${offsetRange.fromOffset}:${offsetRange.untilOffset}")
        .mkString(",")
        
        
        LogHelper.log.debug("offsetsRangesStr : " + offsetsRangesStr); 
        System.out.println("offsetsRangesStr : " + offsetsRangesStr )
         rdd.foreachPartition(partition =>  
          {
            partition.foreach(record => 
              {
                // does the actual processing of data
                // implement your processing logic here... 
                // use connection pooling for underlined resources for saving/persisting data into noSql, kafka.
                // resources are maintained in processing nodes and not in driver node. 
                LogHelper.log.debug("rdd :" + record)
              })
          });
       

      });

   }
   
   def peristOffset(offsetArray: Array[OffsetRange]) : Boolean = { 
     
       val zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
       val partitionCount =offsetArray.size; 
       ZkUtils.updatePersistentPath(zkClient, zkGroupTopicDirs.consumerOffsetDir + "/no_of_partition", partitionCount.toString());
     
       offsetArray.foreach(topic =>  
         {
          ZkUtils.updatePersistentPath(zkClient,zkGroupTopicDirs.consumerOffsetDir + "/partitions/" + topic.partition,topic.untilOffset.toString())
         })   
     true;
   }

  def readOffset() : Option[Map[TopicAndPartition, Long]] = {
    
    val zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
    val partitionMetaNode = zkGroupTopicDirs.consumerOffsetDir + "/no_of_partition"
    var fromOffset = Map[TopicAndPartition, Long]()

    try {
        val no_of_partition = ZkUtils.readData(zkClient,partitionMetaNode)._1.toInt

        for (n <- 0 to no_of_partition-1) {
            var offset = ZkUtils.readData(zkClient,zkGroupTopicDirs.consumerOffsetDir + "/partitions/" + n.toString() )._1
            System.out.println(zkGroupTopicDirs.consumerOffsetDir + "/partitions/" + n.toString() + ":"   + offset);
            fromOffset += (new TopicAndPartition(topics , n.toInt) -> offset.toLong);
        }

    }
    catch {
           

      case ex: KeeperException => { fromOffset = null; System.out.println("some eror1" + ex) }
      case ex : Throwable  => { fromOffset = null; System.out.println("some eror2" + ex) }
      
      
    }
    finally {
      System.out.println("some eror" )
    }
       Option(fromOffset)
       
  }
  
  def persistOffset() = {
       val zkGroupTopicDirs = new ZKGroupTopicDirs("test" ," topic") 

    val zookeeperHosts = "192.168.20.1:2181"; // If multiple zookeeper then -> String zookeeperHosts = "192.168.20.1:2181,192.168.20.2:2181";
    val sessionTimeOutInMs = 15 * 1000; // 15 secs
    val connectionTimeOutInMs = 10 * 1000; // 10 secs
    val zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
    ZkUtils.createPersistentPath(zkClient, "consumer/topic/partition", "10")
    ZkUtils.readData(zkClient, zkGroupTopicDirs.consumerGroupDir)

  }

  
  
  
  
  def main(args: Array[String]) {
    
    val ssc = new StreamingContext(sparkConf, Seconds(2));
    val storedOffsets = readOffset() 
    
    val dsStream = storedOffsets match {
      case None => // start from the latest offsets
        KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
    ssc, kafkaParams, topicsSet)
      case Some(fromOffset) => // start from previously saved offsets
        val messageHandler = (mmd: MessageAndMetadata[String, String]) => (mmd.key, mmd.message)
        KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder
          , (String, String)](ssc, kafkaParams, fromOffset, messageHandler)
    }
    
    

    // Start the computation
    processData(dsStream)
    ssc.start()
    ssc.awaitTermination()

    
  }
  


}


