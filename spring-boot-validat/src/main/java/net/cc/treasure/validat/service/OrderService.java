package net.cc.treasure.validat.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.cc.treasure.validat.entity.OrderRequest;
import net.cc.treasure.validat.entity.ValidationResult;
import net.cc.treasure.validat.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private JsonSchemaValidator schemaValidator;

    /**
     * 处理订单请求
     *
     * <p>流程：</p>
     * <ol>
     *   <li>将请求对象转换为JSON字符串</li>
     *   <li>使用JSON Schema进行校验</li>
     *   <li>校验通过后执行业务逻辑</li>
     * </ol>
     *
     * @param request 订单请求对象
     * @return 处理结果消息
     * @throws ValidationException 校验失败时抛出
     */
    public String processOrder(OrderRequest request) {
        log.info("========================================");
        log.info("开始处理订单请求");
        log.info("========================================");

        // 第1步：转换为JSON字符串
        String jsonString = JSON.toJSONString(request);
        log.debug("请求JSON: {}", jsonString);

        // 第2步：使用Schema进行校验
        ValidationResult validationResult = schemaValidator.validate(jsonString);

        // 第3步：检查校验结果
        if (!validationResult.isValid()) {
            log.warn("订单数据校验失败");
            log.warn("错误列表: {}", validationResult.getErrors());

            // 抛出校验异常，包含所有错误信息
            String errorMessage = "数据校验失败: " + validationResult.getAllErrorsAsString();
            throw new ValidationException(errorMessage);
        }

        log.info("✅ 订单数据校验通过");

        // 第4步：执行业务逻辑
        String result = executeBusinessLogic(request);

        log.info("订单处理完成");
        log.info("========================================");

        return result;
    }

    /**
     * 执行订单业务逻辑
     *
     * <p>这里是实际的业务处理代码，例如：</p>
     * <ul>
     *   <li>保存订单到数据库</li>
     *   <li>发送通知消息</li>
     *   <li>调用其他服务</li>
     * </ul>
     *
     * @param request 订单请求
     * @return 处理结果
     */
    private String executeBusinessLogic(OrderRequest request) {
        log.info("执行业务逻辑...");

        // 示例：打印订单信息
        log.info("订单标题: {}", request.getTitle());
        log.info("订单ID: {}", request.getOrderInfo().getOrderId());
        log.info("城市: {}", request.getOrderInfo().getCityName());

        // TODO: 这里添加实际的业务逻辑
        // 例如：
        // 1. orderRepository.save(order);
        // 2. messageService.sendNotification(order);
        // 3. otherService.processOrder(order);

        return "订单处理成功，订单ID: " + request.getOrderInfo().getOrderId();
    }

    /**
     * 仅校验订单数据，不执行业务逻辑
     *
     * <p>用于客户端在提交前先进行数据校验</p>
     *
     * @param request 订单请求对象
     * @return 校验结果
     */
    public ValidationResult validateOrder(OrderRequest request) {

        log.debug("执行订单数据校验（不处理业务）");

        String jsonString = JSON.toJSONString(request);
        ValidationResult result = schemaValidator.validate(jsonString);

        if (result.isValid()) {
            log.debug("校验通过");
        } else {
            log.debug("校验失败: {}", result.getErrors());
        }

        return result;
    }

    /**
     * 直接校验JSON字符串
     *
     * <p>适用于客户端直接发送JSON字符串的场景</p>
     *
     * @param jsonString JSON字符串
     * @return 校验结果
     */
    public ValidationResult validateJson(String jsonString) {
        log.debug("执行JSON字符串校验");
        return schemaValidator.validate(jsonString);
    }
}
