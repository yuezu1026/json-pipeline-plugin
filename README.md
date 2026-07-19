# JSON Pipeline Plugin

自定义 Jenkins Job 类型 —— 创建 Job 时选择 **"JSON Pipeline"**，填写：

| 字段          | 说明                      | 示例                                           |
| ------------- | ------------------------- | ---------------------------------------------- |
| 配置仓库 URL  | 存放 JSON 配置的 Git 仓库 | `https://github.com/myorg/pipeline-config.git` |
| JSON 文件路径 | 仓库中的相对路径          | `prod/deploy.json`                             |
| 分支          | Git 分支                  | `main`                                         |
| Git 凭证      | 私有仓库凭证（可选）      | -                                              |

保存后自动生成 Pipeline 脚本。

## 构建

```bash
mvn clean package
```

生成 `target/json-pipeline-plugin.hpi`，上传到 Jenkins 安装。

## 依赖

- Jenkins ≥ 2.479
- `workflow-job` / `workflow-cps` / `workflow-cps-global-lib`
- `git` plugin
- 共享库 `jenkins-json-pipeline`
