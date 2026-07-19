/**
 * jsonPipelineFromMap - 从 Groovy Map 执行 pipeline
 *
 * 用法:
 *   jsonPipelineFromMap([
 *     name: 'My Pipeline',
 *     agent: 'any',
 *     stages: [
 *       [name: 'Build', steps: [[type: 'echo', message: 'Building...']]]
 *     ]
 *   ])
 */
def call(Map configMap) {
    jsonPipeline(configMap: [pipeline: configMap])
}
