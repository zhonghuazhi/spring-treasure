package net.cc.treasure.validat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Data
@Component
@ConfigurationProperties(prefix = "validator.schema")
public class ValidatorProperties {

    /**
     * Schema文件主路径
     * <p>支持以下格式：</p>
     * <ul>
     *   <li>classpath:schemas/order-schema.json - 类路径（打包在jar内）</li>
     *   <li>/config/schemas/order-schema.json - 绝对路径（Docker挂载目录）</li>
     *   <li>file:/path/to/schema.json - 文件URL</li>
     * </ul>
     */
    private String location;

    /**
     * Schema文件备用路径
     * <p>当主路径不存在时，使用此路径作为fallback</p>
     */
    private String fallbackLocation;

    /**
     * 是否启用Schema缓存
     * <p>启用后可提升校验性能，但修改Schema文件后需重启服务</p>
     */
    private boolean cacheEnabled = true;

    /**
     * Schema文件重新加载间隔（秒）
     * <p>设置为0表示不自动重载，大于0则定期检测文件变更</p>
     */
    private int reloadInterval = 0;
}
