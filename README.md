
# Fingerprint Browser

Fingerprint Browser 是一款桌面应用程序，旨在使用户能够管理和启动具有独特浏览器指纹的多个浏览器配置文件。这对于需要隔离浏览环境或模拟不同设备和用户配置的 Web 测试、开发和隐私保护场景非常有用。

## ✨ 功能

*   **配置文件管理**:
    *   创建新的浏览器配置文件。
    *   编辑现有配置文件的设置（例如代理）。
    *   删除不再需要的配置文件。
*   **浏览器启动**:
    *   使用指定的配置文件启动浏览器实例。
*   **代理配置**:
    *   为每个配置文件设置独立的代理服务器，以通过不同 IP 地址路由流量。
*   **指纹生成**:
    *   （推断）为每个配置文件生成或自定义浏览器指纹（例如 User-Agent、屏幕分辨率等），使每个会话看起来都来自不同的设备。

## 🛠️ 技术栈

*   **核心框架**: Java 21
*   **用户界面**: JavaFX
*   **构建工具**: Apache Maven
*   **日志**: SLF4J, Logback

## 🚀 如何构建和运行

您可以使用 Maven 来构建和运行此应用程序。

1.  **克隆仓库**:
    ```bash
    git clone https://github.com/alterem/FingerBrowser.git
    cd FingerBrowser
    ```

2.  **构建项目**:
    使用 Maven Wrapper 构建项目。这将会下载所有必要的依赖项并编译源代码。
    ```bash
    ./mvnw clean install
    ```

3.  **运行应用程序**:
    构建成功后，使用以下命令来启动应用程序。
    ```bash
    ./mvnw javafx:run
    ```

## 📂 项目结构

```
FingerBrowser/
├── pom.xml                # Maven 项目配置文件
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/basis/fingerbrowser/
│   │   │       ├── FingerprintBrowserApp.java  # JavaFX 主应用程序类
│   │   │       ├── Launcher.java               # 应用程序启动器
│   │   │       ├── controller/                 # JavaFX 控制器
│   │   │       ├── model/                      # 数据模型 (例如 Profile)
│   │   │       ├── service/                    # 业务逻辑服务
│   │   │       └── view/                       # 视图相关类
│   │   └── resources/
│   │       ├── fxml/                           # FXML 布局文件
│   │       ├── styles/                         # CSS 样式表
│   │       └── images/                         # 应用程序图标
│   └── test/
│       └── java/                               # 单元测试
└── logs/                                       # 日志文件输出目录
```
