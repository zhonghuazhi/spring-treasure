package net.cc.treasure.validat.controller;

import lombok.extern.slf4j.Slf4j;
import net.cc.treasure.validat.entity.ApiResponse;
import net.cc.treasure.validat.entity.OrderRequest;
import net.cc.treasure.validat.entity.ValidationResult;
import net.cc.treasure.validat.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 提交订单
     *
     * <p>接口说明：</p>
     * <ul>
     *   <li>请求方式：POST</li>
     *   <li>Content-Type: application/json</li>
     *   <li>请求体：OrderRequest对象的JSON格式</li>
     * </ul>
     *
     * <p>校验规则：</p>
     * <ul>
     *   <li>title: 必填，1-100字符，只能包含中文、英文、数字、空格</li>
     *   <li>orderInfo.orderId: 必填，1-50字符，只能包含字母、数字、下划线、横线</li>
     *   <li>orderInfo.cityName: 必填，必须是预定义的城市名称之一</li>
     *   <li>其他字段：参见JSON Schema配置</li>
     * </ul>
     *
     * <p>示例请求：</p>
     * <pre>
     * {
     *   "title": "测试信息",
     *   "orderInfo": {
     *     "orderId": "aaa",
     *     "cityName": "西安"
     *   }
     * }
     * </pre>
     *
     * @param request 订单请求对象
     * @return 统一响应对象
     */
    @PostMapping("/submit")
    public ApiResponse<String> submitOrder(@RequestBody OrderRequest request) {

        log.info("收到订单提交请求");
        log.info("请求数据: title={}, orderId={}, cityName={}",
                request.getTitle(),
                request.getOrderInfo() != null ? request.getOrderInfo().getOrderId() : null,
                request.getOrderInfo() != null ? request.getOrderInfo().getCityName() : null);

        try {
            // 处理订单（包含校验和业务逻辑）
            String result = orderService.processOrder(request);

            log.info("订单提交成功");
            return ApiResponse.success("订单提交成功", result);

        } catch (Exception e) {
            log.error("订单提交失败: {}", e.getMessage());
            // 异常会被全局异常处理器捕获
            throw e;
        }
    }

    /**
     * 仅校验订单数据
     *
     * <p>此接口仅进行数据校验，不执行业务逻辑</p>
     * <p>适用于客户端在正式提交前先进行数据验证的场景</p>
     *
     * <p>接口说明：</p>
     * <ul>
     *   <li>请求方式：POST</li>
     *   <li>Content-Type: application/json</li>
     *   <li>请求体：OrderRequest对象的JSON格式</li>
     * </ul>
     *
     * @param request 订单请求对象
     * @return 校验结果
     */
    @PostMapping("/validate")
    public ApiResponse<ValidationResult> validateOrder(@RequestBody OrderRequest request) {

        log.info("收到订单校验请求");

        ValidationResult result = orderService.validateOrder(request);

        if (result.isValid()) {
            log.info("订单数据校验通过");
            return ApiResponse.success("数据校验通过", result);
        } else {
            log.warn("订单数据校验失败: {}", result.getErrors());
            return ApiResponse.error("数据校验失败");
        }
    }

    /**
     * 校验JSON字符串
     *
     * <p>直接接收JSON字符串进行校验</p>
     * <p>适用于客户端想直接传递JSON字符串的场景</p>
     *
     * <p>接口说明：</p>
     * <ul>
     *   <li>请求方式：POST</li>
     *   <li>Content-Type: application/json</li>
     *   <li>请求体：纯JSON字符串</li>
     * </ul>
     *
     * <p>示例请求：</p>
     * <pre>
     * {
     *   "jsonString": "{\"title\":\"测试信息\",\"orderInfo\":{\"orderId\":\"aaa\",\"cityName\":\"西安\"}}"
     * }
     * </pre>
     *
     * @param jsonString JSON字符串
     * @return 校验结果
     */
    @PostMapping("/validate-json")
    public ApiResponse<ValidationResult> validateJson(@RequestBody String jsonString) {
        log.info("收到JSON字符串校验请求");
        log.debug("JSON内容: {}", jsonString);

        ValidationResult result = orderService.validateJson(jsonString);

        if (result.isValid()) {
            log.info("JSON数据校验通过");
            return ApiResponse.success("数据校验通过", result);
        } else {
            log.warn("JSON数据校验失败: {}", result.getErrors());
            return ApiResponse.error("数据校验失败");
        }
    }

    /**
     * 健康检查接口
     *
     * <p>用于检查服务是否正常运行</p>
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("服务运行正常");
    }
}
