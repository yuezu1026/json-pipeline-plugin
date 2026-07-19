package com.jenkins.pipeline

/**
 * 参数 Schema 定义 —— 在共享库中预定义流水线需要哪些输入参数，
 * 构建时自动生成 Jenkins 的参数输入表单。
 */
class ParameterSchema implements Serializable {

    String name              // 参数名
    String type              // string / choice / boolean / password / text / file
    String label             // 界面显示名称
    String description       // 帮助说明
    String defaultValue      // 默认值
    boolean required = true  // 是否必填
    String validationRegex   // 可选的正则校验
    List<ParameterOption> options = []  // choice 类型的选项列表

    /**
     * 从 JSON/Map 构建 Schema
     */
    static ParameterSchema fromJson(Map json) {
        def schema = new ParameterSchema(
            name: json.name,
            type: json.type ?: 'string',
            label: json.label ?: json.name,
            description: json.description ?: '',
            defaultValue: json.defaultValue,
            required: json.required != null ? json.required : true,
            validationRegex: json.validationRegex
        )
        json.options?.each { opt ->
            schema.options << ParameterOption.fromJson(opt)
        }
        return schema
    }

    /**
     * 转换为 Jenkins ParameterConfig
     */
    ParameterConfig toParameterConfig() {
        return new ParameterConfig(
            name: this.name,
            type: this.type,
            defaultValue: this.defaultValue,
            description: this.description ?: this.label ?: '',
            choices: this.options?.collect { it.value }
        )
    }
}

class ParameterOption implements Serializable {
    String value
    String label

    static ParameterOption fromJson(Map json) {
        return new ParameterOption(
            value: json.value,
            label: json.label ?: json.value
        )
    }
}
