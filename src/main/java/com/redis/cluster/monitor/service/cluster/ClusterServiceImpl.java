package com.redis.cluster.monitor.service.cluster;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ClusterInfo;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.stereotype.Service;

import com.redis.cluster.monitor.model.cluster.node.Node;
import com.redis.cluster.monitor.model.cluster.slot.Slot;
import com.redis.cluster.monitor.model.info.Info;
import com.redis.cluster.monitor.util.context.RuntimeContainer;
import com.redis.cluster.monitor.util.convert.AppConverters;
import com.redis.cluster.support.core.RedisTemplate;

@Service
public class ClusterServiceImpl implements ClusterService {
	private static final Log logger = LogFactory.getLog(ClusterServiceImpl.class);
	static {
		System.setProperty("line.separator", "\n");
	}
	@Autowired RedisTemplate<String, Object> redisTemplate;
	
	@Override
	public void info() {
		ClusterInfo info = redisTemplate.opsForCluster().getClusterInfo();
		logger.info(info);
		RuntimeContainer.setRetMessage(info);
	}

	@Override
	public void slots() {
		Set<RedisClusterNode> clusterNodes = redisTemplate.opsForCluster().getClusterNodes();
		logger.info(clusterNodes);
		Set<Slot> slots = AppConverters.toSetOfSlot().convert(clusterNodes);
		RuntimeContainer.setRetMessage(slots);
	}

	@Override
	public void nodes() {
		Set<RedisClusterNode> clusterNodes = redisTemplate.opsForCluster().getClusterNodes();
		Set<Node> nodes = AppConverters.toSetOfNode().convert(clusterNodes);
		logger.info(nodes);
		RuntimeContainer.setRetMessage(nodes);
	}

	@Override
	public void nodesInfo() {
		Map<String, Info> infos = new HashMap<String, Info>();
		Properties prop = redisTemplate.opsForCluster().info();
		logger.info(prop);
		
		Enumeration<Object> keys = prop.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Properties subProp = (Properties) prop.get(key);
			Info info = AppConverters.toInfo().convert(subProp);
			infos.put(String.valueOf(key), info);
		}
		
		RuntimeContainer.setRetMessage(infos);
	}

	@Override
	public void nodeInfo(String node) {
		Properties prop = redisTemplate.opsForCluster().info(create(node));
		Info info = AppConverters.toInfo().convert(prop);
		RuntimeContainer.setRetMessage(info);
	}
	
	@Override
	public void activeMasters() {
		Set<RedisClusterNode> clusterNodes = redisTemplate.opsForCluster().getClusterNodes();
		Set<Node> nodes = AppConverters.toSetOfNode().convert(getActiveMasterNodes(clusterNodes));
		logger.info(nodes);
		RuntimeContainer.setRetMessage(nodes);
	}
	
	private RedisClusterNode create(String node){
		String[] hostAndPort = node.split(":");
		return new RedisClusterNode(hostAndPort[0], Integer.parseInt(hostAndPort[1]), null);
	}
	
	private Set<RedisClusterNode> getActiveMasterNodes(Set<RedisClusterNode> nodes) {

		Set<RedisClusterNode> activeMasterNodes = new LinkedHashSet<RedisClusterNode>(nodes.size());
		for (RedisClusterNode node : nodes) {
			if (node.isMaster() && node.isConnected() && !node.isMarkedAsFail()) {
				activeMasterNodes.add(node);
			}
		}
		return activeMasterNodes;
	}
}
