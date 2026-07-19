# Jenkins JSON Pipeline

> 使用 JSON 配置驱动 Jenkins Pipeline 的 Groovy 共享库

## 项目结构

```
jenkins-json-pipeline/
├── src/com/jenkins/pipeline/
│   ├── PipelineConfig.groovy        # 顶层 pipeline 配置模型
│   ├── StageConfig.groovy           # Stage 配置模型
│   ├── StepConfig.groovy            # Step 配置模型
│   ├── ParameterConfig.groovy       # 参数化构建配置模型
│   ├── PostConfig.groovy            # Post 条件配置模型
│   ├── WhenConfig.groovy            # When 条件配置模型
│   ├── TriggerConfig.groovy         # 触发器配置模型
│   ├── OptionsConfig.groovy         # Options 配置模型
│   ├── StepExecutor.groovy          # Step 执行器（将配置映射为真实 step 调用）
│   └── JsonPipelineBuilder.groovy   # Pipeline 构建器（组装 DSL）
├── vars/
│   ├── jsonPipeline.groovy          # 主入口（共享库全局变量）
│   ├── jsonPipelineFromFile.groovy  # 便捷方法：从文件加载
│   ├── jsonPipelineFromJson.groovy  # 便捷方法：从 JSON 字符串加载
│   └── jsonPipelineFromMap.groovy   # 便捷方法：从 Map 加载
├── resources/pipeline-samples/
│   ├── sample-pipeline.json         # 多环境构建部署示例
│   └── simple-pipeline.json         # Hello World 简单示例
└── Jenkinsfile                      # 示例 Jenkinsfile
```

## 快速开始

### 1. 在 Jenkins 中配置共享库

在 **Manage Jenkins → Configure System → Global Pipeline Libraries** 中添加：

| 设置项                 | 值                      |
| ---------------------- | ----------------------- |
| Name                   | `jenkins-json-pipeline` |
| Default version        | `main`                  |
| Retrieval method       | Modern SCM              |
| Source Code Management | Git                     |
| Project Repository     | `<your-git-repo-url>`   |

### 2. 在 Jenkinsfile 中使用

```groovy
@Library('jenkins-json-pipeline') _

// 从 JSON 文件加载 pipeline 配置
jsonPipeline {
    configFile = 'resources/pipeline-samples/sample-pipeline.json'
}
```

### 3. JSON 配置语法

#### 完整示例

```json
{
  "pipeline": {
    "name": "我的流水线",
    "agent": "any",
    "parameters": [
      {
        "name": "BRANCH",
        "type": "string",
        "defaultValue": "master",
        "description": "分支名"
      }
    ],
    "environment": {
      "APP_NAME": "my-app"
    },
    "tools": {
      "maven": "maven3",
      "jdk": "jdk11"
    },
    "stages": [
      {
        "name": "编译",
        "steps": [
          { "type": "echo", "message": "开始编译..." },
          { "type": "sh", "command": "mvn clean compile" }
        ],
        "post": {
          "failure": [{ "type": "echo", "message": "编译失败!" }]
        }
      },
      {
        "name": "并行构建",
        "parallel": [
          {
            "name": "任务A",
            "steps": [{ "type": "echo", "message": "A" }]
          },
          {
            "name": "任务B",
            "steps": [{ "type": "echo", "message": "B" }]
          }
        ]
      }
    ],
    "post": {
      "always": [{ "type": "echo", "message": "流水线结束" }]
    }
  }
}
```

#### 支持的 Step 类型

| type                     | 说明                | 参数示例                                                                     |
| ------------------------ | ------------------- | ---------------------------------------------------------------------------- |
| `echo`                   | 打印消息            | `{ "type": "echo", "message": "hello" }`                                     |
| `sh`                     | Shell 脚本          | `{ "type": "sh", "command": "mvn test" }`                                    |
| `bat`                    | Windows 批处理      | `{ "type": "bat", "command": "dir" }`                                        |
| `checkout`               | 检出代码            | `{ "type": "checkout" }`                                                     |
| `dir`                    | 切换目录（嵌套）    | `{ "type": "dir", "path": "src", "steps": [...] }`                           |
| `retry`                  | 重试（嵌套）        | `{ "type": "retry", "count": 3, "steps": [...] }`                            |
| `timeout`                | 超时（嵌套）        | `{ "type": "timeout", "time": 10, "unit": "MINUTES", "steps": [...] }`       |
| `sleep`                  | 等待                | `{ "type": "sleep", "time": 5, "unit": "SECONDS" }`                          |
| `stash` / `unstash`      | 文件暂存            | `{ "type": "stash", "name": "libs", "includes": "*.jar" }`                   |
| `writeFile` / `readFile` | 读写文件            | `{ "type": "writeFile", "file": "out.txt", "text": "data" }`                 |
| `junit`                  | 测试报告            | `{ "type": "junit", "testResults": "**/*.xml" }`                             |
| `archiveArtifacts`       | 归档产物            | `{ "type": "archiveArtifacts", "artifacts": "**/*.jar" }`                    |
| `publishHTML`            | HTML 报告           | `{ "type": "publishHTML", "reportDir": "reports", "reportFiles": "*.html" }` |
| `build`                  | 触发下游 Job        | `{ "type": "build", "job": "downstream-job" }`                               |
| `input`                  | 人工审批            | `{ "type": "input", "message": "确认发布?" }`                                |
| `mail`                   | 发送邮件            | `{ "type": "mail", "to": "a@b.com", "subject": "...", "body": "..." }`       |
| `deleteDir`              | 删除工作目录        | `{ "type": "deleteDir" }`                                                    |
| `fileExists`             | 检查文件            | `{ "type": "fileExists", "file": "pom.xml" }`                                |
| `error`                  | 抛出错误            | `{ "type": "error", "message": "出错了!" }`                                  |
| `withCredentials`        | 凭证绑定（嵌套）    | `{ "type": "withCredentials", "bindings": [...], "steps": [...] }`           |
| `withEnv`                | 环境变量（嵌套）    | `{ "type": "withEnv", "env": ["KEY=val"], "steps": [...] }`                  |
| `container`              | Docker 容器（嵌套） | `{ "type": "container", "name": "maven", "steps": [...] }`                   |
| `script`                 | 执行 Groovy 脚本    | `{ "type": "script", "block": "println 'hello'" }`                           |

#### When 条件

```json
{
  "when": {
    "branch": "master",
    "environment": "ENABLE_DEPLOY",
    "expression": "return params.DEPLOY == 'yes'",
    "allOf": [{ "branch": "master" }, { "environment": "PRODUCTION" }],
    "anyOf": [{ "branch": "master" }, { "branch": "develop" }]
  }
}
```

#### 参数类型

| type           | 说明                       |
| -------------- | -------------------------- |
| `string`       | 字符串参数                 |
| `text`         | 多行文本参数               |
| `booleanParam` | 布尔参数                   |
| `choice`       | 下拉选择（需配 `choices`） |
| `password`     | 密码参数                   |
| `file`         | 文件参数                   |
| `run`          | 运行参数                   |

### 4. 使用方式

```groovy
// 方式 1: 闭包配置
jsonPipeline {
    configFile = 'path/to/pipeline.json'
}

// 方式 2: 直接传入 JSON 字符串
jsonPipeline('{"pipeline":{"name":"My Pipeline","stages":[...]}}')

// 方式 3: 从文件路径
jsonPipeline('path/to/pipeline.json')

// 方式 4: 便捷方法
jsonPipelineFromFile('path/to/pipeline.json')
jsonPipelineFromJson('{"pipeline":{...}}')
jsonPipelineFromMap([name: 'My Pipeline', stages: [...]])
```

## 设计理念

1. **JSON 即配置** —— Pipeline 的 stages、steps、条件等全部用 JSON 声明，不再需要修改 Groovy 代码
2. **模型驱动** —— JSON 解析为 `PipelineConfig` 等强类型模型，构建器读模型生成 pipeline
3. **可扩展** —— 在 `StepExecutor` 中添加新的 `case` 即可支持自定义 step 类型
4. **条件执行** —— 支持 `when` 条件（branch、expression、allOf、anyOf 等）
5. **后置处理** —— 支持 stage 级别和 pipeline 级别的 `post` 条件块
