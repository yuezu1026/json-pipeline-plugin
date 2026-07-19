/**
 * jsonPipeline - Jenkins 共享库全局变量入口
 *
 * ============================================================
 * 用法 1: 闭包指定配置
 * ============================================================
 *   @Library('jenkins-json-pipeline') _
 *
 *   // A. 从外部 Git 仓库加载 JSON 配置（推荐）
 *   jsonPipeline {
 *       configRepo   = 'https://github.com/myorg/pipeline-config.git'
 *       configPath   = 'prod/deploy.json'
 *       configBranch = 'main'   // 可选，默认 main
 *   }
 *
 *   // B. 从本地文件加载
 *   jsonPipeline {
 *       configFile = 'pipeline.json'
 *   }
 *
 *   // C. 直接写 JSON
 *   jsonPipeline {
 *       configJson = '{"pipeline": {"name": "test", "stages": [...]}}'
 *   }
 *
 * ============================================================
 * 用法 2: 直接传参
 * ============================================================
 *   jsonPipeline('config/pipeline.json')                     // 本地文件
 *   jsonPipeline('{"pipeline": {...}}')                       // JSON 字符串
 *
 * ============================================================
 * 高级功能: 预定义参数 Schema
 * ============================================================
 *  在 JSON 配置中定义 "schema" 字段，jenkins 在构建时自动生成参数表单:
 *
 *  {
 *    "pipeline": {
 *      "name": "部署流水线",
 *      "schema": [
 *        {"name": "BRANCH", "type": "string",  "label": "分支",     "required": true,  "defaultValue": "main"},
 *        {"name": "ENV",    "type": "choice",  "label": "目标环境", "required": true,  "options": [
 *          {"value": "dev", "label": "开发环境"},
 *          {"value": "stg", "label": "预发布"},
 *          {"value": "prd", "label": "生产环境"}
 *        ]},
 *        {"name": "DRY_RUN","type": "boolean", "label": "仅预览",   "required": false, "defaultValue": false}
 *      ],
 *      "stages": [...]
 *    }
 *   }
 */

import com.jenkins.pipeline.PipelineConfig
import com.jenkins.pipeline.JsonPipelineBuilder

/**
 * 无参数调用 —— 从 body 闭包中获取配置
 */
def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    PipelineConfig pipelineConfig

    // ---- 优先级: configRepo > configFile > configJson > configMap ----
    if (config.configRepo && config.configPath) {
        // ========== 从外部 Git 仓库加载 JSON 配置 (Part A) ==========
        echo "[jsonPipeline] 从外部仓库加载配置: ${config.configRepo} → ${config.configPath}"
        checkout([
            $class: 'GitSCM',
            branches: [[name: config.configBranch ?: 'main']],
            userRemoteConfigs: [[url: config.configRepo]]
        ])
        pipelineConfig = PipelineConfig.fromFile(config.configPath)

    } else if (config.configJson) {
        pipelineConfig = PipelineConfig.fromJson(config.configJson)

    } else if (config.configFile) {
        pipelineConfig = PipelineConfig.fromFile(config.configFile)

    } else if (config.configMap) {
        pipelineConfig = PipelineConfig.fromJson(groovy.json.JsonOutput.toJson(config.configMap))

    } else {
        error('jsonPipeline: 需要提供 configRepo+configPath、configJson、configFile 或 configMap 参数')
    }

    // ---- 参数化构建 ----
    applyParameters(pipelineConfig)

    def builder = new JsonPipelineBuilder(this)
    builder.build(pipelineConfig)
}

/**
 * 带参数调用 —— 传入 JSON 字符串或文件路径
 */
def call(String jsonOrPath) {
    def pipelineConfig

    if (jsonOrPath.contains('{') && jsonOrPath.contains('}')) {
        pipelineConfig = PipelineConfig.fromJson(jsonOrPath)
    } else {
        pipelineConfig = PipelineConfig.fromFile(jsonOrPath)
    }

    applyParameters(pipelineConfig)

    def builder = new JsonPipelineBuilder(this)
    builder.build(pipelineConfig)
}

/**
 * 应用参数定义: schema (预定义) 优先，否则使用 JSON 中的 parameters
 */
private void applyParameters(PipelineConfig pipelineConfig) {
    def paramDefs = []

    if (pipelineConfig.schema) {
        // ========== 使用 Schema 生成参数表单 (Part B) ==========
        echo "[jsonPipeline] 应用预定义参数 Schema: ${pipelineConfig.schema.size()} 个参数"
        paramDefs = pipelineConfig.schema.collect { schema ->
            buildParameter(schema.toParameterConfig())
        }
    } else if (pipelineConfig.parameters) {
        // ========== 使用 JSON 中定义的 parameters ==========
        paramDefs = pipelineConfig.parameters.collect { param ->
            buildParameter(param)
        }
    }

    if (paramDefs) {
        properties([parameters(paramDefs)])
    }
}

/**
 * 构建 parameter 定义
 */
private def buildParameter(com.jenkins.pipeline.ParameterConfig param) {
    switch (param.type) {
        case 'string':
            return string(name: param.name, defaultValue: param.defaultValue?.toString() ?: '', description: param.description)
        case 'text':
            return text(name: param.name, defaultValue: param.defaultValue?.toString() ?: '', description: param.description)
        case 'booleanParam':
        case 'boolean':
            return booleanParam(name: param.name, defaultValue: param.defaultValue ?: false, description: param.description)
        case 'choice':
            return choice(name: param.name, choices: param.choices as String[], description: param.description)
        case 'password':
            return password(name: param.name, defaultValue: param.defaultValue?.toString() ?: '', description: param.description)
        case 'file':
            return file(name: param.name, description: param.description)
        case 'run':
            return run(name: param.name, description: param.description, projectName: param.defaultValue?.toString() ?: '', filter: 'ALL')
        default:
            return string(name: param.name, defaultValue: param.defaultValue?.toString() ?: '', description: param.description)
    }
}
