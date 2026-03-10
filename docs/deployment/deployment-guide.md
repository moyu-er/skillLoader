# 部署指南

## Maven 仓库部署

### 1. 发布到 Maven Central

#### 配置 pom.xml

```xml
<project>
    ...
    
    <distributionManagement>
        <snapshotRepository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
        </snapshotRepository>
        <repository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
        </repository>
    </distributionManagement>
    
    <build>
        <plugins>
            <!-- Source plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Javadoc plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.6.3</version>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- GPG plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>sign</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 发布命令

```bash
# 1. 设置 GPG 密钥
export GPG_TTY=$(tty)

# 2. 编译测试
mvn clean verify

# 3. 发布到 staging
mvn clean deploy -P release

# 4. 登录 https://s01.oss.sonatype.org/ 关闭并发布 staging repository
```

---

## 私有仓库部署

### 部署到 Nexus/Artifactory

```bash
# 配置 settings.xml
<settings>
    <servers>
        <server>
            <id>company-nexus</id>
            <username>your-username</username>
            <password>your-password</password>
        </server>
    </servers>
</settings>

# 发布
mvn clean deploy -DrepositoryId=company-nexus
```

---

## Docker 部署

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 复制项目文件
COPY . .

# 构建
RUN ./mvnw clean package -DskipTests

# 运行示例
CMD ["java", "-jar", "target/skill-loader-core-1.0.0-SNAPSHOT.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  skillloader:
    build: .
    volumes:
      - ./skills:/app/skills:ro
    environment:
      - JAVA_OPTS=-Xmx512m
```

---

## Spring Boot 集成部署

### 作为依赖使用

```xml
<dependency>
    <groupId>com.skillloader</groupId>
    <artifactId>skill-loader-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置类

```java
@Configuration
public class SkillLoaderConfig {
    
    @Bean
    public SkillLoader skillLoader() {
        return SkillLoader.createDefault();
    }
}
```

### REST API 控制器

```java
@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {
    
    private final SkillLoader loader;
    
    public SkillController(SkillLoader loader) {
        this.loader = loader;
    }
    
    @GetMapping
    public List<SkillSummary> listSkills() {
        return loader.discover().stream()
            .map(s -> new SkillSummary(s.name(), s.description(), s.tags()))
            .collect(Collectors.toList());
    }
    
    @GetMapping("/{name}")
    public ResponseEntity<SkillDetail> getSkill(@PathVariable String name) {
        try {
            SkillContent content = loader.load(name);
            return ResponseEntity.ok(new SkillDetail(
                content.metadata(),
                content.markdownContent(),
                content.resources()
            ));
        } catch (SkillNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{name}/resources/{resourceName}")
    public ResponseEntity<String> getResource(
            @PathVariable String name,
            @PathVariable String resourceName) {
        try {
            SkillContent content = loader.load(name);
            ResourceRef resource = content.resources().stream()
                .filter(r -> r.name().equals(resourceName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(resourceName));
            
            String content = Files.readString(Paths.get(resource.uri()));
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

---

## Kubernetes 部署

### deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: skillloader-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: skillloader
  template:
    metadata:
      labels:
        app: skillloader
    spec:
      containers:
      - name: skillloader
        image: your-registry/skillloader:1.0.0
        ports:
        - containerPort: 8080
        volumeMounts:
        - name: skills-volume
          mountPath: /app/skills
          readOnly: true
      volumes:
      - name: skills-volume
        configMap:
          name: skills-configmap
---
apiVersion: v1
kind: Service
metadata:
  name: skillloader-service
spec:
  selector:
    app: skillloader
  ports:
  - port: 80
    targetPort: 8080
```

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: skills-configmap
data:
  git-workflow/SKILL.md: |
    ---
    name: git-workflow
    description: Git workflow guidelines
    ---
    # Git Workflow
    ...
```

---

## Serverless 部署

### AWS Lambda

```java
public class SkillHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final SkillLoader loader = SkillLoader.createDefault();
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request, 
            Context context) {
        
        String skillName = request.getPathParameters().get("skillName");
        
        try {
            SkillContent content = loader.load(skillName);
            
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(content.markdownContent());
        } catch (SkillNotFoundException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withBody("Skill not found");
        }
    }
}
```

---

## 监控和日志

### 健康检查

```java
@Component
public class SkillHealthIndicator implements HealthIndicator {
    
    private final SkillLoader loader;
    
    public SkillHealthIndicator(SkillLoader loader) {
        this.loader = loader;
    }
    
    @Override
    public Health health() {
        int skillCount = loader.discover().size();
        
        return Health.up()
            .withDetail("skillCount", skillCount)
            .build();
    }
}
```

### 指标监控

```java
@Component
public class SkillMetrics {
    
    private final MeterRegistry registry;
    private final SkillLoader loader;
    
    public SkillMetrics(MeterRegistry registry, SkillLoader loader) {
        this.registry = registry;
        this.loader = loader;
        
        // 注册指标
        Gauge.builder("skills.total", loader, l -> l.discover().size())
            .register(registry);
    }
}
```

---

## 安全建议

1. **只读挂载**：skills 目录应以只读方式挂载
2. **资源限制**：设置 CPU/内存限制
3. **网络隔离**：使用网络策略限制访问
4. **日志审计**：记录所有 skill 加载操作
