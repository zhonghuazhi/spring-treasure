package net.cc.treasure.validat.exception;

import lombok.extern.slf4j.Slf4j;
import net.cc.treasure.validat.entity.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理数据校验异常
     *
     * <p>当JSON Schema校验失败时触发</p>
     *
     * @param e 校验异常
     * @return 错误响应
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(ValidationException e) {
        log.error("数据校验异常: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * 处理Schema加载异常
     *
     * <p>当Schema文件加载失败时触发</p>
     *
     * @param e Schema加载异常
     * @return 错误响应
     */
    @ExceptionHandler(SchemaLoadException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleSchemaLoadException(SchemaLoadException e) {
        log.error("Schema加载异常: {}", e.getMessage(), e);
        return ApiResponse.serverError("服务配置错误，请联系管理员");
    }

    /**
     * 处理JSON解析异常
     *
     * <p>当请求体JSON格式错误时触发</p>
     *
     * @param e JSON解析异常
     * @return 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("JSON解析异常: {}", e.getMessage());

        String message = "JSON格式错误";
        if (e.getMessage() != null) {
            // 提取更友好的错误信息
            if (e.getMessage().contains("Cannot deserialize")) {
                message = "JSON数据类型不匹配";
            } else if (e.getMessage().contains("Unexpected character")) {
                message = "JSON格式不正确，请检查括号、引号等符号";
            }
        }

        return ApiResponse.error(400, message);
    }

    /**
     * 处理方法参数校验异常
     *
     * <p>当使用@Valid注解校验参数失败时触发</p>
     *
     * @param e 参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数校验异常: {}", e.getMessage());

        // 提取第一个校验错误信息
        String message = "参数校验失败";
        if (e.getBindingResult().hasErrors()) {
            message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }

        return ApiResponse.error(400, message);
    }

    /**
     * 处理IllegalArgumentException异常
     *
     * <p>当方法参数不合法时触发</p>
     *
     * @param e 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * 处理其他未预期的异常
     *
     * <p>作为兜底的异常处理，捕获所有未被其他Handler处理的异常</p>
     *
     * @param e 异常对象
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return ApiResponse.serverError("系统内部错误，请稍后重试");
    }

}
