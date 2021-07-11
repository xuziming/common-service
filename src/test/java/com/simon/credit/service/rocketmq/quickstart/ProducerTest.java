package com.simon.credit.service.rocketmq.quickstart;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * RocketMQ生产者
 * <p>
 * RocketMQ有多种配置方式可以令客户端找到 NameServer, 然后通过 NameServer 再找到 Broker，分别如下，
 * 优先级由高到低，高优优先级会覆盖低优先级
 *
 * 1、代码中指定 Name Server 地址
 * producer.setNamesrvAddr("192.168.0.1:9876;192.168.0.2:9876");
 *
 * 2、启动参数指定
 * -Drocketmq.namesrv.addr=192.168.0.1:9876;192.168.0.2:9876
 *
 * 3、环境变量指定 Name Server 地址
 * export NAMESRV_ADDR=192.168.0.1:9876;192.168.0.2:9876
 *
 * 4、HTTP 静态服务器寻址（默认）
 * 如果以上三种都没有设置name server的地址，客户端启动后先会访问一个静态http服务器获取name server的地址，然后会启动一个定时任务访问这个静态 HTTP 服务器，
 * 地址为：http://jmenv.tbsite.net:8080/rocketmq/nsaddr
 *
 * 上面是默认的地址，当然你也可以更改，做如下设置：
 * System.setProperty("rocketmq.namesrv.domain","localhost");
 * System.setProperty("rocketmq.namesrv.domain.subgroup"，"nameServer")
 *
 * 或者启动参数指定：
 * -Drocketmq.namesrv.domain=localhost
 * -Drocketmq.namesrv.domain.subgroup=nameServer
 *
 * 以上设置后http服务器地址就变成：
 * http://localhsot:8080/rocketmq/nameServer
 *
 * 这个 URL 的返回内容格式如下：
 * 192.168.0.1:9876;192.168.0.2:9876
 *
 * 客户端每隔 2 分钟访问一次这个 HTTP 服务器，并更新本地的 Name Server 地址。
 * 推荐使用 HTTP 静态服务器寻址方式，好处是客户端部署简单，且 Name Server 集群可以热升级。
 * </p>
 */
public class ProducerTest {

	public static void main(String[] args) {
		DefaultMQProducer producer = new DefaultMQProducer("test_producer_default");
		// producer.setNamesrvAddr("127.0.0.1:9876");

		try {
			producer.start();
			for (int i = 0; i < 10; i++) {
				try {
					String bodyStr = "hello world " + System.nanoTime();
					Message msg = new Message("TopicTest", bodyStr.getBytes("UTF-8"));
					SendResult sendResult = producer.send(msg);
					System.out.println(JSON.toJSONString(sendResult));
					System.out.println("id:" + sendResult.getMsgId() + " status:" + sendResult.getSendStatus());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			producer.shutdown();
		}
	}

}