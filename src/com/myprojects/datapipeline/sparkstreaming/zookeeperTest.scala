package com.myprojects.datapipeline.sparkstreaming

import kafka.serializer.StringDecoder
import kafka.message.MessageAndMetadata
import kafka.utils.ZkUtils
import kafka.utils.ZKGroupTopicDirs
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import kafka.utils.ZKStringSerializer$


object zookeeperTest {
  
         val zkGroupTopicDirs = new ZKGroupTopicDirs("consumer" ,"topic") 

    def persistOffset() = {
      System.out.println("pat" + zkGroupTopicDirs.consumerOffsetDir)

    val zookeeperHosts = "localhost:2181"; // If multiple zookeeper then -> String zookeeperHosts = "192.168.20.1:2181,192.168.20.2:2181";
    val sessionTimeOutInMs = 15 * 1000; // 15 secs
    val connectionTimeOutInMs = 10 * 1000; // 10 secs
    val zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
    ZkUtils.updatePersistentPath(zkClient, zkGroupTopicDirs.consumerOffsetDir + "/no_of_partitions" , "5")
    ZkUtils.updatePersistentPath(zkClient, zkGroupTopicDirs.consumerOffsetDir + "partitions/1", "5")
    ZkUtils.updatePersistentPath(zkClient, zkGroupTopicDirs.consumerOffsetDir + "partitions/2", "10")
    ZkUtils.updatePersistentPath(zkClient, zkGroupTopicDirs.consumerOffsetDir + "partitions/3", "15")
    ZkUtils.updatePersistentPath(zkClient, zkGroupTopicDirs.consumerOffsetDir +  "partitions/4", "20")
    ZkUtils.updatePersistentPath(zkClient, zkGroupTopicDirs.consumerOffsetDir + "partitions/5", "25")
  }
    
  def read(path: String) = {
    val zookeeperHosts = "localhost:2181"; // If multiple zookeeper then -> String zookeeperHosts = "192.168.20.1:2181,192.168.20.2:2181";
    val sessionTimeOutInMs = 15 * 1000; // 15 secs
    val connectionTimeOutInMs = 10 * 1000; // 10 secs
    val zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
    val partitionMetaNode = path + "/no_of_partitions"
    val no_of_partition = ZkUtils.readData(zkClient,partitionMetaNode)._1.toInt
    for (n <- 1 to no_of_partition) {
      
      var offset = ZkUtils.readData(zkClient,path + "partitions/" + n.toString() )._1
      System.out.println(path + "/partitions/" + n.toString() + ":"   + offset);
    }

    }
  
 def main(args: Array[String]) {
  persistOffset();
   read(zkGroupTopicDirs.consumerOffsetDir);
 }
 
}