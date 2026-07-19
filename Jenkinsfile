/**
 * Jenkinsfile 示例 —— 使用 jsonPipeline 共享库
 *
 * 前提：在 Jenkins 管理页面中配置好 Global Pipeline Library
 *   名称: jenkins-json-pipeline
 *   默认版本: main
 *   加载方式: Modern SCM (Git)
 *
 * 或使用 @Library 注解动态加载:
 *   @Library('jenkins-json-pipeline@main') _
 */

// ============================================================
// 方式 1: 从文件加载 JSON 配置（推荐）
// ============================================================
jsonPipeline {
    configFile = 'resources/pipeline-samples/sample-pipeline.json'
}

// ============================================================
// 方式 2: 内联 JSON 配置
// ============================================================
// jsonPipeline('''
// {
//   "pipeline": {
//     "name": "内联 Pipeline",
//     "agent": "any",
//     "stages": [
//       {
//         "name": "Hello",
//         "steps": [
//           { "type": "echo", "message": "Hello Jenkins!" }
//         ]
//       }
//     ]
//   }
// }
// ''')

// ============================================================
// 方式 3: 便捷方法
// ============================================================
// jsonPipelineFromFile('resources/pipeline-samples/sample-pipeline.json')
// jsonPipelineFromJson('{ "pipeline": { "name": "...", ... } }')
// jsonPipelineFromMap([name: 'My Pipeline', stages: [...]])

// ============================================================
// 方式 4: 传统 Jenkinsfile + JSON 混合 —— 在 script 块中使用
// ============================================================
// pipeline {
//     agent any
//     stages {
//         stage('初始化') {
//             steps {
//                 script {
//                     // 动态加载 JSON pipeline
//                     jsonPipelineFromFile('config/custom.json')
//                 }
//             }
//         }
//     }
// }
