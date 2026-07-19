package com.jenkins.pipeline

/**
 * 参数化构建配置
 */
class ParameterConfig implements Serializable {

    String name
    String type          // string, text, booleanParam, choice, password, file, run
    Object defaultValue
    String description
    List<String> choices = []   // choice 类型的选项

    static ParameterConfig fromJson(Map json) {
        return new ParameterConfig(
            name: json.name,
            type: json.type ?: 'string',
            defaultValue: json.defaultValue ?: json.default,
            description: json.description ?: '',
            choices: json.choices ?: []
        )
    }
}
