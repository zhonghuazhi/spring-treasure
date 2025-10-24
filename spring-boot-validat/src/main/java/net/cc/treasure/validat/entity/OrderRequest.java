package net.cc.treasure.validat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    /**
     * 标题信息
     */
    private String title;

    /**
     * 订单详细信息
     */
    private OrderInfo orderInfo;

    /**
     * 订单信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo {

        /**
         * 订单ID
         */
        private String orderId;

        /**
         * 城市名称
         */
        private String cityName;

        /**
         * 订单金额（可选）
         */
        private Double amount;

        /**
         * 订单数量（可选）
         */
        private Integer quantity;

        /**
         * 订单类型（可选）
         * <p>可选值：NORMAL-普通订单, URGENT-加急订单, SPECIAL-特殊订单</p>
         */
        private String orderType;

        /**
         * 客户姓名（可选）
         */
        private String customerName;

        /**
         * 联系电话（可选）
         */
        private String phone;

        /**
         * 邮箱地址（可选）
         */
        private String email;

        /**
         * 收货地址（可选）
         */
        private Address address;

        /**
         * 订单标签（可选）
         */
        private java.util.List<String> tags;

        /**
         * 创建时间（可选）
         */
        private String createTime;

        /**
         * 订单状态（可选）
         * <p>1-待支付, 2-已支付, 3-已发货, 4-已完成, 5-已取消</p>
         */
        private Integer status;
    }

    /**
     * 地址信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {

        /**
         * 省份
         */
        private String province;

        /**
         * 城市
         */
        private String city;

        /**
         * 详细地址
         */
        private String detail;
    }
}
