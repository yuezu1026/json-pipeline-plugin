/**
 * jsonPipeline - Jenkins 共享库全局变量入口
 *
 * 用法 (在 Jenkinsfile 中):
 *   @Library('jenkins-json-pipeline') _
 *   jsonPipeline {
 *       configFile = 'pipeline.json'  // 或 configJson = '...'
 *   }
 *
 * 也支持在 Jenkinsfile 中直接传入 JSON:
 *   jsonPipeline('''{ "pipeline": { ... } }''')
 *
 * 或者传入 JSON 文件路径:
 *   jsonPipeline('config/pipeline.json')
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

    if (config.configJson) {
        // 从 JSON 字符串解析
        pipelineConfig = PipelineConfig.fromJson(config.configJson)
    } else if (config.configFile) {
        // 从 JSON 文件解析
        pipelineConfig = PipelineConfig.fromFile(config.configFile)
    } else if (config.configMap) {
        // 从 Map 直接构建（用于 Groovy 代码中构建配置）
        pipelineConfig = PipelineConfig.fromJson(groovy.json.JsonOutput.toJson(config.configMap))
    } else {
        error('jsonPipeline: 需要提供 configJson、configFile 或 configMap 参数')
    }

    // 参数化构建
    if (pipelineConfig.parameters) {
        properties([
            parameters(pipelineConfig.parameters.collect { param ->
                buildParameter(param)
            })
        ])
    }

    def builder = new JsonPipelineBuilder(this)
    builder.build(pipelineConfig)
}

/**
 * 带参数调用 —— 传入 JSON 字符串或文件路径
 */
def call(String jsonOrPath) {
    def pipelineConfig

    if (jsonOrPath.contains('{') && jsonOrPath.contains('}')) {
        // 看起来是 JSON 字符串
        pipelineConfig = PipelineConfig.fromJson(jsonOrPath)
    } else {
        // 当作文件路径
        pipelineConfig = PipelineConfig.fromFile(jsonOrPath)
    }

    def builder = new JsonPipelineBuilder(this)
    builder.build(pipelineConfig)
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
