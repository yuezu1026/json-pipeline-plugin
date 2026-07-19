package com.jenkins.pipeline

/**
 * Step 配置模型
 *
 * 支持的 type:
 *   echo, sh, bat, checkout, dir, fileExists, error, isUnix, libraryResource,
 *   mail, deleteDir, stash, unstash, writeFile, readFile, retry, sleep, timeout,
 *   waitUntil, withCredentials, withEnv, script, container, build, input, publishHTML,
 *   junit, archiveArtifacts, parallel
 */
class StepConfig implements Serializable {

    String type                          // step 类型
    Map<String, Object> params = [:]     // step 参数
    List<StepConfig> children = []       // 嵌套 steps（如 dir, timeout, retry 等）

    /**
     * 深度转换 LazyMap → HashMap，确保所有嵌套 Map 都可序列化
     */
    private static Map<String, Object> deepCopyMap(Map map) {
        if (map == null) return [:]
        Map<String, Object> result = new HashMap<>()
        map.each { key, value ->
            result[key] = deepCopyValue(value)
        }
        return result
    }

    private static List deepCopyList(List list) {
        if (list == null) return []
        return list.collect { deepCopyValue(it) }
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof Map) {
            return deepCopyMap((Map) value)
        } else if (value instanceof List) {
            return deepCopyList((List) value)
        }
        return value
    }

    static StepConfig fromJson(Map json) {
        def step = new StepConfig()
        step.type = json.type ?: json.remove('type')

        // 特殊处理：嵌套结构的 step（children）
        if (json.steps) {
            json.steps.each { childJson ->
                step.children << StepConfig.fromJson(childJson)
            }
            json.remove('steps')
        }

        // 剩余字段作为 params —— 深拷贝确保可序列化
        step.params = deepCopyMap(json)
        return step
    }
}
