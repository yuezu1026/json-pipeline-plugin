/**
 * jsonPipelineFromJson - 从 JSON 字符串执行 pipeline
 *
 * 用法:
 *   jsonPipelineFromJson('''
 *   {
 *     "pipeline": {
 *       "name": "My Pipeline",
 *       "stages": [...]
 *     }
 *   }
 *   ''')
 */
def call(String jsonStr) {
    jsonPipeline(configJson: jsonStr)
}
