# OAuth 协议各版本对比 & SSO 资料整理

---

## 一、OAuth 协议演进

### 1.1 OAuth 1.0 (2007)

| 项目 | 说明 |
|------|------|
| 核心机制 | 基于**签名**（HMAC-SHA1 / RSA-SHA1）验证请求 |
| 流程 | 获取临时凭证 → 用户授权 → 用临时凭证+签名换取访问令牌 |
| 优点 | 安全性高，签名防篡改 |
| 缺点 | 实现复杂，签名计算繁琐，不支持刷新令牌，不区分委托粒度 |
| 代表 | 早期 Twitter API、Google Contacts API |

### 1.2 OAuth 1.0a (2009)

- 修复了 1.0 的**会话劫持漏洞**
- 增加 `oauth_verifier` 参数，防止回调劫持
- 是目前实际使用的 1.0 版本

### 1.3 OAuth 2.0 (2012, RFC 6749)

| 项目 | 说明 |
|------|------|
| 核心机制 | 基于 **Token + HTTPS**，不再使用签名 |
| 授权模式 | 4 种授权模式（见下表） |
| 刷新令牌 | 支持 `refresh_token` 机制 |
| 优点 | 实现简单、支持细粒度权限（scope）、支持第三方应用 |
| 缺点 | 必须依赖 HTTPS；协议本身不向后兼容 1.0；存在已知安全陷阱（CSRF、开放重定向等） |

#### OAuth 2.0 四种授权模式

| 授权模式 | 适用场景 | 流程简述 |
|---------|---------|---------|
| **授权码模式** (Authorization Code) | 有后端的 Web 应用 | 浏览器跳转授权页 → 拿到 code → 后端用 code 换 token |
| **简化模式** (Implicit) | 纯前端 SPA（已不推荐） | 浏览器跳转授权页 → 直接返回 token（在 URL fragment 中） |
| **密码模式** (Resource Owner Password) | 高度信任的客户端 | 用户直接把用户名密码给客户端 → 客户端换 token |
| **客户端凭证模式** (Client Credentials) | 服务间调用 / API 访问 | 客户端用自己的凭证直接换 token |

### 1.4 OAuth 2.1 (草案中)

| 项目 | 说明 |
|------|------|
| 定位 | 整合 OAuth 2.0 + 最佳安全实践 (Security BCP) |
| 关键变化 | **强制 PKCE**（所有客户端）；**移除 Implicit 模式**；**强制 HTTPS**；Refresh Token 需绑定客户端 |
| 状态 | IETF 草案，尚未正式发布为 RFC |

---

## 二、OAuth 2.0 核心扩展协议

| 协议 | RFC | 说明 |
|------|-----|------|
| **PKCE** | RFC 7636 | 授权码增强，防拦截攻击，SPA 和移动端必备 |
| **JWT Access Token** | RFC 9068 | 定义 JWT 格式的 access_token 标准 |
| **Token Revocation** | RFC 7009 | 令牌撤销接口 |
| **Token Introspection** | RFC 7662 | 令牌内省（验证令牌有效性） |
| **Device Authorization** | RFC 8628 | 设备授权流程（如 TV、IoT） |
| **Pushed Authorization Requests (PAR)** | RFC 9126 | 将授权请求参数通过后端推送，提升安全性 |

---

## 三、OAuth 1.0a vs 2.0 对比

| 维度 | OAuth 1.0a | OAuth 2.0 |
|------|-----------|-----------|
| 安全机制 | HMAC/RSA 签名 | HTTPS + Bearer Token |
| 实现复杂度 | 高（签名、nonce、timestamp） | 低（HTTP 请求即可） |
| 刷新令牌 | 不支持 | 支持 |
| 权限粒度 | 粗粒度 | 通过 scope 细粒度控制 |
| 适用客户端 | 主要 Web | Web / Mobile / IoT / SPA |
| 令牌格式 | 不透明字符串 | 不透明字符串 / JWT |
| 安全性 | 签名防篡改，安全性高 | 依赖 HTTPS，有已知陷阱需额外防护 |

---

## 四、SSO（单点登录）

### 4.1 核心概念

SSO（Single Sign-On）允许用户在**一个身份提供者（IdP）登录一次**，即可访问**多个应用系统（SP）**，无需重复登录。

### 4.2 主流 SSO 协议对比

| 协议 | 类型 | 适用场景 | 特点 |
|------|------|---------|------|
| **CAS** | 重定向 | 企业内部系统 | 简单，基于浏览器重定向，票据验证 |
| **SAML 2.0** | XML + 重定向/POST | 企业级、跨组织 | 成熟、功能全面，但 XML 报文复杂 |
| **OpenID Connect (OIDC)** | JSON + REST | 互联网应用、移动端 | 基于 OAuth 2.0，轻量，JWT 格式，易集成 |
| **Kerberos** | 票据 + 对称加密 | 企业内网（Windows AD） | 无需 HTTPS，但仅限内网环境 |

### 4.3 CAS 流程

```
用户 → SP → CAS Server（登录）→ 获得 ST（Service Ticket）
     → SP 拿 ST 去 CAS Server 验证 → 验证通过 → 用户登录成功
```

### 4.4 SAML 2.0 流程

```
用户 → SP → 重定向到 IdP → IdP 认证用户 → 生成 SAML Assertion（XML）
     → 通过浏览器 POST 回 SP → SP 验证签名 → 建立会话
```

### 4.5 OIDC 流程

```
用户 → 应用 → 重定向到 IdP（OAuth 2.0 授权流程）
     → IdP 认证 → 返回 id_token (JWT) + access_token
     → 应用验证 id_token → 建立会话
```

---

## 五、OIDC vs SAML 2.0

| 维度 | OIDC | SAML 2.0 |
|------|------|----------|
| 基础协议 | OAuth 2.0 | XML 签名 + 断言 |
| 数据格式 | JSON / JWT | XML |
| 复杂度 | 低 | 高 |
| 移动端友好度 | 高 | 低（XML 解析重） |
| 属性传递 | id_token + UserInfo 端点 | Attribute Statement |
| 会话管理 | 支持前端登出、Session 管理 | 支持 Single Logout |
| 适用场景 | 互联网应用、SPA、移动端 | 企业级、传统 Web 应用 |
| 代表产品 | Google / Auth0 / Keycloak | Azure AD / Okta / Shibboleth |

---

## 六、常见开源 SSO / IdP 方案

| 方案 | 支持协议 | 说明 |
|------|---------|------|
| **Keycloak** | OIDC, SAML 2.0, CAS | Red Hat 开源，功能最全，Java 生态 |
| **Authelia** | OIDC, SAML 2.0 | Go 语言，轻量，适合自托管 |
| **Casdoor** | OIDC, SAML, CAS | Go 语言，国产开源，UI 友好 |
| **Authentik** | OIDC, SAML | Python/Django，适合 homelab |
| **Spring Authorization Server** | OAuth 2.0, OIDC | Spring 官方，Java 生态深度集成 |

---

## 七、OAuth 2.0 安全最佳实践

### 7.1 PKCE（Proof Key for Code Exchange）

```
1. 客户端生成随机 code_verifier
2. 对 code_verifier 做 SHA256 得到 code_challenge
3. 授权请求时携带 code_challenge
4. 换 token 时携带原始 code_verifier
5. 服务端验证 code_verifier 与之前的 code_challenge 匹配
```

### 7.2 安全要点清单

| 要点 | 说明 |
|------|------|
| 强制 HTTPS | 所有通信必须加密 |
| state 参数 | 防 CSRF，每次授权请求携带随机 state |
| PKCE | 防授权码拦截 |
| 短有效期 Access Token | 减少令牌泄露风险 |
| Refresh Token 轮换 | 每次刷新后旧 refresh_token 失效 |
| 令牌绑定 | 将 token 绑定到特定客户端（Sender-Constrained Token） |

---

## 八、JWT (JSON Web Token) 结构

```
Header.Payload.Signature
```

| 部分 | 内容 |
|------|------|
| Header | `{"alg": "RS256", "typ": "JWT"}` |
| Payload | Claims（iss, sub, aud, exp, iat, scope 等） |
| Signature | `RSASHA256(base64(header) + "." + base64(payload), privateKey)` |

**常用 Claims：**

| Claim | 含义 |
|-------|------|
| `iss` | 签发者（Issuer） |
| `sub` | 主题（Subject，即用户标识） |
| `aud` | 受众（Audience，即目标应用） |
| `exp` | 过期时间（Expiration Time） |
| `iat` | 签发时间（Issued At） |
| `scope` | 授权范围 |

---

## 九、选型建议

### 9.1 按场景选择

| 场景 | 推荐方案 |
|------|---------|
| 互联网应用 / SPA / 移动端 | OAuth 2.0 + OIDC + PKCE |
| 企业内部多系统 SSO | CAS 或 OIDC |
| 企业跨组织联邦认证 | SAML 2.0 |
| 服务间 API 调用 | OAuth 2.0 Client Credentials |
| IoT / 设备端 | OAuth 2.0 Device Authorization |
| Spring Boot 项目 | Spring Authorization Server + OIDC |

### 9.2 Spring Boot 集成路径

```xml
<!-- Spring Security OAuth2 Client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- 作为授权服务器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
```

---

## 十、参考资料

- [RFC 6749 - OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)
- [RFC 7636 - PKCE](https://datatracker.ietf.org/doc/html/rfc7636)
- [RFC 8414 - OAuth 2.0 Authorization Server Metadata](https://datatracker.ietf.org/doc/html/rfc8414)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [OAuth 2.1 Draft](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1)
- [Spring Authorization Server 文档](https://docs.spring.io/spring-authorization-server/reference/)
- [Keycloak 官方文档](https://www.keycloak.org/documentation)
- [SAML 2.0 技术规范](https://docs.oasis-open.org/security/saml/v2.0/)
