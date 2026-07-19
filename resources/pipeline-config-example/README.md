# pipeline-config

存放 Jenkins JSON Pipeline 的配置文件仓库。

## 目录结构

```
pipeline-config/
├── dev/
│   └── build.json
├── staging/
│   └── deploy.json
└── prod/
    └── deploy.json
```

## JSON 配置格式

```json
{
  "pipeline": {
    "name": "流水线名称",
    "schema": [
      {
        "name": "BRANCH",
        "type": "string",
        "label": "构建分支",
        "required": true,
        "defaultValue": "main"
      }
    ],
    "stages": [
      { "name": "Stage 1", "steps": [...] }
    ]
  }
}
```

## 使用方式

在 `jenkins-json-pipeline` 共享库中引用：

```groovy
@Library('jenkins-json-pipeline') _

jsonPipeline {
    configRepo   = 'https://github.com/myorg/pipeline-config.git'
    configPath   = 'prod/deploy.json'
    configBranch = 'main'
}
```
