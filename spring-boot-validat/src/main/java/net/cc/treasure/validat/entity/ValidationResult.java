package net.cc.treasure.validat.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult {

    /**
     * 是否校验通过
     */
    private boolean valid;

    /**
     * 错误信息列表
     * <p>当校验失败时，包含所有校验错误的详细描述</p>
     */
    private List<String> errors;

    public ValidationResult() {
        this.errors = new ArrayList<>();
    }

    /**
     * 添加错误信息
     *
     * @param error 错误描述
     */
    public void addError(String error) {
        this.errors.add(error);
    }

    /**
     * 获取第一个错误信息
     *
     * @return 第一个错误，如果没有错误返回null
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    /**
     * 获取所有错误信息的字符串表示
     *
     * @return 错误信息字符串，多个错误用分号分隔
     */
    public String getAllErrorsAsString() {
        return String.join("; ", errors);
    }
}
