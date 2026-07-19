package com.jenkins.pipeline

/**
 * Step 执行器 —— 将 StepConfig 映射为 Jenkins Pipeline 实际步骤调用
 *
 * 所有方法接收 `steps`（即 Jenkins Pipeline 的 steps 上下文）作为参数，
 * 确保在声明式 pipeline 的 `script` 块中仍可调用。
 */
class StepExecutor implements Serializable {

    /**
     * 执行单个 step
     * @param stepCtx  Jenkins Pipeline 的 this（或 steps 对象）
     * @param step     StepConfig 配置
     */
    static void execute(def stepCtx, StepConfig step) {
        switch (step.type) {
            case 'echo':
                stepCtx.echo(step.params.message ?: '')
                break

            case 'sh':
                def label = step.params.label ?: ''
                def returnStatus = step.params.returnStatus ?: false
                def script = step.params.script ?: step.params.command ?: ''
                if (label) {
                    stepCtx.sh(script: script, label: label, returnStatus: returnStatus)
                } else {
                    stepCtx.sh(script: script, returnStatus: returnStatus)
                }
                break

            case 'bat':
                def script = step.params.script ?: step.params.command ?: ''
                def returnStatus = step.params.returnStatus ?: false
                stepCtx.bat(script: script, returnStatus: returnStatus)
                break

            case 'checkout':
                stepCtx.checkout(stepCtx.scm)
                break

            case 'dir':
                stepCtx.dir(step.params.path ?: step.params.dir) {
                    step.children.each { child -> execute(stepCtx, child) }
                }
                break

            case 'fileExists':
                stepCtx.fileExists(step.params.file ?: step.params.path)
                break

            case 'error':
                stepCtx.error(step.params.message ?: 'Unknown error')
                break

            case 'isUnix':
                stepCtx.isUnix()
                break

            case 'libraryResource':
                stepCtx.libraryResource(step.params.resource ?: step.params.path)
                break

            case 'mail':
                stepCtx.mail(
                    to: step.params.to,
                    subject: step.params.subject ?: '',
                    body: step.params.body ?: ''
                )
                break

            case 'deleteDir':
                stepCtx.deleteDir()
                break

            case 'stash':
                stepCtx.stash(
                    name: step.params.name,
                    includes: step.params.includes ?: '',
                    excludes: step.params.excludes ?: ''
                )
                break

            case 'unstash':
                stepCtx.unstash(step.params.name)
                break

            case 'writeFile':
                stepCtx.writeFile(
                    file: step.params.file ?: step.params.path,
                    text: step.params.text ?: '',
                    encoding: step.params.encoding ?: 'UTF-8'
                )
                break

            case 'readFile':
                stepCtx.readFile(
                    file: step.params.file ?: step.params.path,
                    encoding: step.params.encoding ?: 'UTF-8'
                )
                break

            case 'retry':
                def count = (step.params.count ?: step.params.times ?: 1) as int
                stepCtx.retry(count) {
                    step.children.each { child -> execute(stepCtx, child) }
                }
                break

            case 'sleep':
                def time = step.params.time ?: step.params.seconds ?: 1
                def unit = step.params.unit ?: 'SECONDS'
                stepCtx.sleep(time: time, unit: unit)
                break

            case 'timeout':
                def time = step.params.time ?: step.params.minutes ?: 1
                def unit = step.params.unit ?: 'MINUTES'
                stepCtx.timeout(time: time, unit: unit) {
                    step.children.each { child -> execute(stepCtx, child) }
                }
                break

            case 'waitUntil':
                stepCtx.waitUntil(initialRecurrencePeriod: step.params.initialRecurrencePeriod ?: 500) {
                    step.children.each { child -> execute(stepCtx, child) }
                }
                break

            case 'withCredentials':
                def bindings = []
                step.params.bindings?.each { binding ->
                    def b = []
                    binding.each { k, v -> b << "${k}(${v})" }
                    bindings << b.join(',')
                }
                stepCtx.withCredentials(bindings) {
                    step.children.each { child -> execute(stepCtx, child) }
                }
                break

            case 'withEnv':
                stepCtx.withEnv(step.params.env ?: []) {
                    step.children.each { child -> execute(stepCtx, child) }
                }
                break

            case 'script':
                def scriptBlock = step.params.block ?: step.params.script
                if (scriptBlock instanceof String) {
                    stepCtx.evaluate(scriptBlock)
                }
                break

            case 'container':
                stepCtx.container(step.params.name ?: step.params.image) {
                    step.children.each { child -> execute(stepCtx, child) }
                }
                break

            case 'build':
                stepCtx.build(
                    job: step.params.job ?: step.params.project,
                    parameters: step.params.parameters ?: [],
                    propagate: step.params.propagate ?: true,
                    wait: step.params.wait ?: true
                )
                break

            case 'input':
                stepCtx.input(
                    message: step.params.message ?: 'Proceed?',
                    id: step.params.id ?: 'Approval',
                    submitter: step.params.submitter ?: '',
                    ok: step.params.ok ?: 'Proceed'
                )
                break

            case 'publishHTML':
                stepCtx.publishHTML(
                    target: [
                        allowMissing: step.params.allowMissing ?: false,
                        alwaysLinkToLastBuild: step.params.alwaysLinkToLastBuild ?: false,
                        keepAll: step.params.keepAll ?: true,
                        reportDir: step.params.reportDir ?: '',
                        reportFiles: step.params.reportFiles ?: 'index.html',
                        reportName: step.params.reportName ?: 'HTML Report'
                    ]
                )
                break

            case 'junit':
                stepCtx.junit(
                    testResults: step.params.testResults ?: '**/target/test-results/*.xml',
                    skipPublishingChecks: step.params.skipPublishingChecks ?: false,
                    allowEmptyResults: step.params.allowEmptyResults ?: true
                )
                break

            case 'archiveArtifacts':
                stepCtx.archiveArtifacts(
                    artifacts: step.params.artifacts ?: step.params.pattern ?: '**/*',
                    fingerprint: step.params.fingerprint ?: false,
                    allowEmptyArchive: step.params.allowEmptyArchive ?: true
                )
                break

            case 'configFileProvider':
                stepCtx.configFileProvider(
                    step.params.files ?: step.params.configFiles ?: []
                ) {
                    step.children.each { child -> execute(stepCtx, child) }
                }
                break

            default:
                stepCtx.echo("[WARN] Unknown step type: ${step.type}")
                break
        }
    }

    /**
     * 执行 post 条件中的步骤
     */
    static void executePost(def stepCtx, PostConfig post, String condition) {
        def steps = post."$condition"
        if (steps) {
            steps.each { step -> execute(stepCtx, step) }
        }
    }
}
