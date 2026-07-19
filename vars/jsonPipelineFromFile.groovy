/**
 * jsonPipelineFromFile - 从 JSON 文件路径加载并执行 pipeline
 *
 * 用法:
 *   jsonPipelineFromFile('config/my-pipeline.json')
 *   或
 *   jsonPipelineFromFile {
 *       path = 'config/my-pipeline.json'
 *   }
 */
def call(String filePath) {
    jsonPipeline(configFile: filePath)
}

def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    jsonPipeline(configFile: config.path ?: config.file ?: config.configFile)
}
