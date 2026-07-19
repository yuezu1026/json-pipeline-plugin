package com.jenkins.pipeline

/**
 * Post 条件配置
 *
 * 条件: always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, cleanup
 */
class PostConfig implements Serializable {

    List<StepConfig> always = []
    List<StepConfig> changed = []
    List<StepConfig> fixed = []
    List<StepConfig> regression = []
    List<StepConfig> aborted = []
    List<StepConfig> failure = []
    List<StepConfig> success = []
    List<StepConfig> unstable = []
    List<StepConfig> unsuccessful = []
    List<StepConfig> cleanup = []

    static PostConfig fromJson(Map json) {
        def post = new PostConfig()
        ['always', 'changed', 'fixed', 'regression', 'aborted',
         'failure', 'success', 'unstable', 'unsuccessful', 'cleanup'].each { condition ->
            if (json[condition]) {
                def steps = []
                json[condition].each { stepJson ->
                    steps << StepConfig.fromJson(stepJson)
                }
                post."$condition" = steps
            }
        }
        return post
    }
}
