# 日志配置说明

## 📁 日志文件位置

所有日志文件保存在项目根目录下的 `logs/` 文件夹中：

```
Brief-Wisdom/
└── logs/
    ├── brief-wisdom.log              # 主日志文件（INFO及以上级别）
    ├── brief-wisdom-error.log        # 错误日志文件（仅ERROR级别）
    ├── brief-wisdom-ai.log           # AI模块专用日志
    └── brief-wisdom-resume.log       # 简历模块专用日志
```

## 📋 日志文件说明

### 1. 主日志文件 (`brief-wisdom.log`)

- **记录内容**: 所有 INFO、WARN、ERROR 级别的日志
- **滚动策略**:
    - 按天分割：`brief-wisdom-2026-07-05.0.log`
    - 单文件最大: 100MB
    - 保留天数: 30天
    - 总大小上限: 10GB

### 2. 错误日志文件 (`brief-wisdom-error.log`)

- **记录内容**: 仅 ERROR 级别的错误日志
- **滚动策略**:
    - 按天分割：`brief-wisdom-error-2026-07-05.0.log`
    - 单文件最大: 100MB
    - 保留天数: 90天（错误日志保留更久）
    - 总大小上限: 5GB

### 3. AI 模块日志 (`brief-wisdom-ai.log`)

- **记录内容**: `com.mouhin.brief.wisdom.ai` 包下的所有日志（DEBUG级别）
- **用途**: 追踪 AI 调用、模型切换、Token 消耗等
- **滚动策略**: 同主日志文件

### 4. 简历模块日志 (`brief-wisdom-resume.log`)

- **记录内容**: `com.mouhin.brief.wisdom.resume` 包下的所有日志（DEBUG级别）
- **用途**: 追踪简历 CRUD、AI 润色请求等
- **滚动策略**: 同主日志文件

## 🔧 日志配置特性

### 自动清理

- 启动时自动清理超过保留天数的旧日志
- 当日志总大小超过上限时，自动删除最旧的日志

### 实时生效

- 配置文件修改后 60 秒内自动重新加载（无需重启应用）
- 配置扫描间隔: `scanPeriod="60 seconds"`

### 控制台输出

- 开发环境：同时输出到控制台和文件
- 彩色显示：不同日志级别用不同颜色区分
    - ERROR: 红色
    - WARN: 黄色
    - INFO: 绿色
    - DEBUG: 蓝色

## 📝 日志格式

```
2026-07-05 14:30:25.123 [http-nio-8090-exec-1] INFO  c.m.b.w.r.c.ResumeAiController - AI文本润色请求, fieldType=description, textLength=15
```

格式说明：

- `2026-07-05 14:30:25.123`: 时间戳
- `[http-nio-8090-exec-1]`: 线程名
- `INFO`: 日志级别
- `c.m.b.w.r.c.ResumeAiController`: 类名（缩写）
- 后面是具体的日志消息

## 🎯 特殊配置

### MyBatis SQL 日志

- Mapper 层的 SQL 语句会以 DEBUG 级别输出
- 方便调试数据库操作

### Spring Security 日志

- 设置为 WARN 级别，减少不必要的认证日志

### Chrome DevTools 警告抑制

- 将无资源告警设置为 ERROR 级别，避免控制台刷屏

## 💡 使用建议

### 查看最新日志

```bash
# Mac/Linux
tail -f logs/brief-wisdom.log

# Windows (PowerShell)
Get-Content logs/brief-wisdom.log -Wait -Tail 50
```

### 搜索特定错误

```bash
# 查找包含 "AI" 的错误日志
grep "AI" logs/brief-wisdom-error.log

# 查找今天的错误日志
grep "2026-07-05" logs/brief-wisdom-error.log
```

### 查看 AI 相关日志

```bash
# 实时查看 AI 模块日志
tail -f logs/brief-wisdom-ai.log
```

### 清理日志

```bash
# 手动清理所有日志（谨慎使用）
rm -rf logs/*.log
```

## ⚙️ 调整日志级别

如需调整某个包的日志级别，编辑 `logback-spring.xml`：

```xml
<!-- 示例：将 AI 模块日志级别改为 INFO -->
<logger name="com.mouhin.brief.wisdom.ai" level="INFO" additivity="false">
    <appender-ref ref="AI_FILE"/>
    <appender-ref ref="CONSOLE"/>
</logger>
```

可用级别（从低到高）：

- `TRACE`: 最详细
- `DEBUG`: 调试信息
- `INFO`: 一般信息
- `WARN`: 警告
- `ERROR`: 错误

## 🚀 生产环境建议

1. **日志级别**: 建议设置为 `INFO`，避免 DEBUG 日志过多
2. **磁盘空间**: 确保有足够的磁盘空间存储日志
3. **日志监控**: 可集成 ELK、Prometheus 等日志监控系统
4. **敏感信息**: 注意不要在日志中记录密码、密钥等敏感信息

---

**最后更新**: 2026-07-05  
**配置文件**: `brief-wisdom-web/src/main/resources/logback-spring.xml`
