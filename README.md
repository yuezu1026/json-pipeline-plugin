# Jenkins JSON Pipeline

> 使用 JSON 配置驱动 Jenkins Pipeline 的 Groovy 共享库

## 整体架构

```
json-pipeline-plugin (Jenkins Plugin)     ← Part C: 自定义 Job Type
  +-- New Item 出现 "JSON Pipeline" 选项

jenkins-json-pipeline (Shared Library)    ← Part B: 参数 Schema
  +-- @Library + jsonPipeline {}

pipeline-config (独立 Git 仓库)            ← Part A: JSON 配置
  +-- prod/deploy.json / dev/build.json
```

## 项目结构

```
jenkins-json-pipeline/
├── src/com/jenkins/pipeline/
│   ├── PipelineConfig.groovy        # 顶层配置模型（含 schema/configRepo）
│   ├── StageConfig.groovy           # Stage 配置模型
│   ├── StepConfig.groovy            # Step 配置模型（深度序列化安全）
│   ├── ParameterConfig.groovy       # 参数化构建配置模型
│   ├── ParameterSchema.groovy       # ★ 预定义参数 Schema
│   ├── PostConfig.groovy / WhenConfig.groovy
│   ├── TriggerConfig.groovy / OptionsConfig.groovy
│   ├── StepExecutor.groovy          # Step 执行器
│   └── JsonPipelineBuilder.groovy   # Pipeline 构建器
├── vars/
│   ├── jsonPipeline.groovy          # ★ 主入口
│   ├── jsonPipelineFromFile.groovy
│   ├── jsonPipelineFromJson.groovy
│   └── jsonPipelineFromMap.groovy
├── resources/
│   ├── pipeline-samples/            # 示例 JSON
│   └── pipeline-config-example/     # ★ 独立配置仓库模板
└── Jenkinsfile
```

## 快速开始

### 1. 配置共享库

Manage Jenkins → System → Global Pipeline Libraries → Add:

| 设置            | 值                                                             |
| --------------- | -------------------------------------------------------------- |
| Name            | `jenkins-json-pipeline`                                        |
| Default version | `main`                                                         |
| SCM             | Git → `https://github.com/yuezu1026/jenkins-json-pipeline.git` |

> 需预先安装插件: `Pipeline: Shared Groovy Libraries`

### 2. 三种使用方式

#### A. 从外部 Git 仓库加载 JSON（推荐 ★）

```groovy
@Library('jenkins-json-pipeline') _

jsonPipeline {
    configRepo   = 'https://github.com/myorg/pipeline-config.git'
    configPath   = 'prod/deploy.json'
    configBranch = 'main'
}
```

#### B. 从本地文件加载

```groovy
jsonPipeline {
    configFile = 'resources/pipeline-samples/simple-pipeline.json'
}
```

#### C. 直接传入 JSON 字符串

```groovy
jsonPipeline '''
{
  "pipeline": {
    "name": "test",
    "stages": [
      { "name": "Hello", "steps": [{ "type": "echo", "params": { "message": "hi" } }] }
    ]
  }
}
'''
```

### 3. JSON 配置语法（含 Schema 参数）

```json
{
  "pipeline": {
    "name": "部署流水线",
    "schema": [
      {
        "name": "BRANCH",
        "type": "string",
        "label": "分支",
        "required": true,
        "defaultValue": "main"
      },
      {
        "name": "ENV",
        "type": "choice",
        "label": "环境",
        "required": true,
        "options": [
          { "value": "dev", "label": "开发" },
          { "value": "prd", "label": "生产" }
        ]
      },
      {
        "name": "DRY_RUN",
        "type": "boolean",
        "label": "仅预览",
        "defaultValue": false
      }
    ],
    "stages": [
      {
        "name": "构建",
        "steps": [
          { "type": "echo", "params": { "message": "branch=${params.BRANCH}" } }
        ]
      },
      {
        "name": "部署",
        "when": { "expression": "params.DRY_RUN == false" },
        "steps": [
          { "type": "echo", "params": { "message": "deploy to ${params.ENV}" } }
        ]
      }
    ]
  }
}
```

## Schema 参数类型

| type       | 说明     | Jenkins 对应参数   |
| ---------- | -------- | ------------------ |
| `string`   | 文本输入 | String Parameter   |
| `choice`   | 下拉选择 | Choice Parameter   |
| `boolean`  | 复选框   | Boolean Parameter  |
| `password` | 密码     | Password Parameter |
| `text`     | 多行文本 | Text Parameter     |

## 支持的 Step 类型

| type                     | 说明              | 嵌套 steps |
| ------------------------ | ----------------- | ---------- |
| `echo`                   | 打印消息          | -          |
| `sh` / `bat`             | Shell / Batch     | -          |
| `checkout`               | 检出代码          | -          |
| `dir`                    | 切换目录          | ✓          |
| `retry`                  | 重试              | ✓          |
| `timeout`                | 超时控制          | ✓          |
| `sleep`                  | 暂停等待          | -          |
| `stash` / `unstash`      | 暂存 / 恢复文件   | -          |
| `writeFile` / `readFile` | 读写文件          | -          |
| `junit`                  | JUnit 测试报告    | -          |
| `archiveArtifacts`       | 归档构建产物      | -          |
| `build`                  | 触发下游 Job      | -          |
| `input`                  | 人工审批          | -          |
| `mail`                   | 发送邮件          | -          |
| `script`                 | 执行 Groovy 脚本  | -          |
| `withCredentials`        | 凭证绑定          | ✓          |
| `container`              | Docker 容器内执行 | ✓          |

## 本地 Docker 开发

```bash
cd docker
docker compose up -d
# 访问 http://localhost:8085
```
