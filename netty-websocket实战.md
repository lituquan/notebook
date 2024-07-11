
## 客户端
```java
// 用于握手，消息监听
public class ClientHandler extends SimpleChannelInboundHandler 

// 管理channel: 连接服务端，得到的channel对象，保存系统中，后续发消息用
// 池化：这里不要连接太多

```

## 服务端
```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.77.Final</version>
</dependency>
<dependency>
    <groupId>org.yeauty</groupId>
    <artifactId>netty-websocket-spring-boot-starter</artifactId>
    <version>0.9.5</version>
</dependency>
```

```java
// 监听端口
@ServerEndpoint(path = "/ws", port = "8003")

// 处理消息
@OnMessage
public void onMessage(Session session, String message) {
    log.info("from client:{}", message);
}
```
