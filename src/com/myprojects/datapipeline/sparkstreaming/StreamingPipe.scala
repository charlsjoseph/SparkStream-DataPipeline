package com.myprojects.datapipeline.sparkstreaming

import org.apache.spark.streaming.kafka._
import kafka.serializer.StringDecoder
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.log4j.Logger



import org.apache.kafka.common.serialization.StringDeserializer 
import java.util.concurrent.atomic.LongAccumulator

object LogHelper extends Serializable {      
   @transient lazy val log = Logger.getLogger("debug1")    
}



object StreamingPipe {
  
   val sparkConf = new SparkConf().setAppName("StreamingPipeLine").setMaster("local[2]")
   val checkpoint = "/home/charls/program/checkpoint"
    LogHelper.log.debug("test") 

      val prop = System.getProperty("org.slf4j.simpleLogger.logFile")
      System.out.println("prop : " + prop)
   val topics = "MetricData";
   val brokers = "localhost:9092,localhost:9093"
    // Create direct kafka stream with brokers and topics
   val topicsSet = topics.split(",").toSet
        System.out.println(topicsSet);
    
   val kafkaParams = Map[String, String](
   "bootstrap.servers" -> "localhost:9092,localhost:9093",
   "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
   "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
   "auto.offset.reset" -> "largest",
   "group.id" -> "my_group"
   )                               
   
   def processData(dsStream: InputDStream[(String, String)]) {
     dsStream.foreachRDD(rdd => 
      {
        LogHelper.log.debug("Processing starts: "); 
        LogHelper.log.debug("Rdd : with partition size : " + rdd.partitions.size + " message count in the batch: " + rdd.count); 

        // process the rdd and persist the offset into zookeeper( if you are not using spark check pointing)
        
        val offsetsRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        val offsetsRangesStr = offsetsRanges.map(offsetRange => s"${offsetRange.partition}:${offsetRange.fromOffset}:${offsetRange.untilOffset}")
        .mkString(",")
        LogHelper.log.debug("offsetsRangesStr : " + offsetsRangesStr); 
        
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
          })
         });

   }
   
   
   // This will be invoked if the checkpoint is not found in StreamingContext.getActiveOrCreate() 
   // to create ssc- spark streaming context.
   // please note the processData(dsStream) is the one actually process the streaming data.
   // Typically no code change is expected in this method, modify only processData(dsStream) method in order to change any process logic.
   // If at all, there is a change in this method, you need to clear the spark checkpoint files so that it doesn't use the old implementation.
   def createSC(): StreamingContext = {
    LogHelper.log.debug("Checkpoint not found : Creating a new spark streaming"); 
    val ssc = new StreamingContext(sparkConf, Seconds(4));
    ssc.checkpoint(checkpoint);
    val dsStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
    ssc, kafkaParams, topicsSet)
    processData(dsStream)
    ssc
    
  }
   

  def main(args: Array[String]) {
    val ssc = StreamingContext.getActiveOrCreate(checkpoint, createSC)
    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
  


}


