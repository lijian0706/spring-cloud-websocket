### 背景介绍
- 前段时间项目中有服务端推送的业务场景，首先映入脑海的便是`WebSocket`和`SSE`，相较之下，我们决定使用`WebSocket`，但经过仔细分析与尝试以后，发现如下难点亟需攻克，否则方案就得搁浅。
    - `zuul 1.0`对`WebSocket`支持并不理想，连接一段时间后就会自动掉线：`Whoops! Lost connection to http://localhost:7000/web-socket/ws`
    - 由于上线后业务微服务会有多个无状态实例，而`WebSocket`是长连接且有状态的，它只会与其中一个实例保持连接，其他实例无法获得它的连接，因此无法主动向其推送消息。

### 使用`Spring Cloud Gateway`代替`Spring Cloud zuul`
- 对于第一个难点，我们查阅官方文档发现，`zuul`从2.0开始才会支持`WebSocket`，而`Spring Cloud`似乎并不打算集成`zuul 2.0`，而是推出了自己的反向代理(智能路由)`Spring Cloud Gateway`，因此我们便决定从`Spring Cloud zuul`切换到`Spring Cloud Gateway`上，对于`Eureka`相关的内容不是本篇文章的重点，请参考源码。
- 添加`Spring Cloud Gateway`依赖，此处没有贴出`jquery`、`bootstrap`相关依赖，请参考源码
```
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```
- 相关配置
```
spring.application.name: gateway

server:
  port: 7000
  compression:
    enabled: true
    mime-types: text/html,text/css,application/javascript,application/json
#spring.resources.static-locations: classpath:/resources/static

spring:
  cloud:
    gateway:
      routes:
      - id: web-socket
        uri: lb://web-socket
        predicates:
        - Path=/web-socket/**
        filters:
        - RewritePath=/web-socket/(?<path>.*), /$\{path}
          # SockJS route
      - id: websocket_sockjs_route
        uri: lb://web-socket
        predicates:
          - Path=/web-socket/info/**
        # Normwal Websocket route
      - id: websocket_route
        uri: lb:ws://web-socket
        predicates:
          - Path=/web-socket/**
```
- 限于文章篇幅，只贴出核心的`js`代码
```
function connect() {
    // var socket = new SockJS('/web-socket/ws?access_token=xxxxx'); // 支持oauth授权
    var socket = new SockJS('/web-socket/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/user/topic/greetings', function (greeting) {
            showGreeting(greeting.body);
        });
    });
}
```

### 多实例下的`WebSocket`配置
- 本项目中有用到消息中间件，最开始的解决方案是：负载到哪个实例，那么由他通过中间件向其他实例广播，通知他们向WebSocket写入数据，所有实例监听到消息，一起向WebSocket发送消息，其实WebSocket只会与其中一个实例建立连接，也就是说，只有其中一个实例写成功，这样便达到了在多实例下，服务端推送的效果。此方案可行，却并不是最优解决方案，最优解决方案是：RabbitMQ+STOMP，下面会进行详细介绍。

##### RabbitMQ的安装
- 使用docker compose 安装RabbitMQ
```
version: '3'
services:
  rabbitmq:
    image: "rabbitmq:3-management"
    hostname: "rabbit"
    environment:
      RABBITMQ_DEFAULT_USER: "rabbitmq"
      RABBITMQ_DEFAULT_PASS: "rabbitmq"
    ports:
      - "15672:15672"
      - "5672:5672"
      - "61613:61613"
    labels:
      NAME: "rabbitmq"
    volumes:
      - ./rabbitmq-isolated.conf:/etc/rabbitmq/rabbitmq.config
```
- 开启`STOMP`
    - 登录容器:`docker exec -it containerId bash`
    - 开启rabbitmq的stomp端口：`rabbitmq-plugins enable rabbitmq_stomp`
##### 业务微服务web-socket的关键代码
- 配置信息，主要是STOMP的配置
```
stomp.server: localhost
stomp.port: 61613
stomp.username: rabbitmq
stomp.password: rabbitmq
```
- WebSocket配置类
```
@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(StompProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private StompProperties stompProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {//配置消息代理  Message Broker 点对点式
        registry.setApplicationDestinationPrefixes("/app") // 配置请求都以/app打头，没有特殊意义，例如：@MessageMapping("/hello")，其实真实路径是/app/hello
                .enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(stompProperties.getServer())
                .setRelayPort(stompProperties.getPort())
                .setClientLogin(stompProperties.getUsername())
                .setClientPasscode(stompProperties.getPassword())
                .setSystemLogin(stompProperties.getUsername())
                .setSystemPasscode(stompProperties.getPassword())
                .setVirtualHost("/")
                .setUserDestinationBroadcast("/topic/greetings")
                .setUserRegistryBroadcast("/topic/greetings");

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").setHandshakeHandler(defaultHandshakeHandler()).withSockJS();//注册STOMP协议的节点 指定使用SockJS协议,setAllowedOrigins 添加允许跨域访问
    }


    private DefaultHandshakeHandler defaultHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                return () -> "lijian"; // 模拟用户 lijian，向该用户推送
            }
        };
    }
}
```

- 测试WebSocket，简单的接收和发送
```
@MessageMapping("/hello")
@SendToUser("/topic/greetings")
public String greeting(String message) {
	log.info("message: {}", message);
	return "hello";
}
```
- 测试接口，用于向指定用户发消息
```
@Autowired
private SimpMessagingTemplate messagingTemplate;

@GetMapping("/test")
public void test(String username){
    messagingTemplate.convertAndSendToUser(username, "/topic/greetings", "hello:"+username);
}
```
- 测试过程
    - 启动服务发现`discovery-server`、`gateway`、业务微服务`web-socket`启动两份实例，端口号分别是8080、8081(`java -jar web-socket.jar --server.port=8081`)
    - 打开页面`http://localhost:7000/110000.html`，点击Connect按钮，打开浏览器控制台，可看到连接成功的信息，点击Send按钮，可向服务端发送消息，服务端控制台我们会看到，只有其中一个实例接收到消息。
    ```
    2019-05-28 22:02:37.233  INFO 8192 --- [boundChannel-55] c.lijian.websocket.WebSocketApplication  : message: ssss
    ```
    - 通过PostMan调用测试接口分别向8080，8081端口发送请求，测试多实例下的服务端推送，发现无论向哪个实例发送，页面都能接收到消息。

### 最后
- 到此处为止，便实现了在多个实例下，向WebSocket发送消息的功能。
- 源码地址：https://github.com/lijian0706/spring-cloud-websocket
- 欢迎转载，转载请附上原文链接
