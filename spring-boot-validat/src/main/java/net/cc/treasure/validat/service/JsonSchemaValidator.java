package net.cc.treasure.validat.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import net.cc.treasure.validat.config.ValidatorProperties;
import net.cc.treasure.validat.entity.ValidationResult;
import net.cc.treasure.validat.exception.SchemaLoadException;
import net.cc.treasure.validat.exception.ValidationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JsonSchemaValidator {

    private final ValidatorProperties properties;
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    /**
     * Schema缓存
     * <p>key: schema路径, value: JsonSchema对象</p>
     */
    private final ConcurrentHashMap<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    /**
     * Schema文件最后修改时间缓存
     * <p>用于检测文件是否变更</p>
     */
    private final ConcurrentHashMap<String, Long> schemaLastModified = new ConcurrentHashMap<>();

    public JsonSchemaValidator(ValidatorProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /**
     * 初始化方法，在Bean创建后执行
     * <p>预加载Schema到缓存中</p>
     */
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("初始化 JSON Schema 校验器");
        log.info("========================================");
        log.info("Schema主路径: {}", properties.getLocation());
        log.info("Schema备用路径: {}", properties.getFallbackLocation());
        log.info("缓存启用: {}", properties.isCacheEnabled());
        log.info("重载间隔: {}秒", properties.getReloadInterval());

        try {
            // 预加载Schema
            loadSchema();
            log.info("Schema加载成功");
        } catch (Exception e) {
            log.error("Schema加载失败: {}", e.getMessage());
            throw new SchemaLoadException("Schema初始化失败", e);
        }

        log.info("========================================");
    }

    /**
     * 校验JSON字符串
     *
     * @param jsonString 待校验的JSON字符串
     * @return 校验结果
     * @throws ValidationException 校验失败时抛出
     */
    public ValidationResult validate(String jsonString) {

        log.debug("开始校验JSON数据");

        try {
            // 解析JSON
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // 获取Schema（支持缓存和热重载）
            JsonSchema schema = getSchema();

            // 执行校验
            Set<ValidationMessage> errors = schema.validate(jsonNode);

            // 构建校验结果
            ValidationResult result = new ValidationResult();
            result.setValid(errors.isEmpty());

            if (!errors.isEmpty()) {
                log.warn("校验失败，错误数量: {}", errors.size());
                for (ValidationMessage error : errors) {
                    String errorMsg = formatErrorMessage(error);
                    result.addError(errorMsg);
                    log.debug("校验错误: {}", errorMsg);
                }
            } else {
                log.debug("校验通过");
            }

            return result;

        } catch (IOException e) {
            log.error("JSON解析失败: {}", e.getMessage());
            throw new ValidationException("JSON格式错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("校验过程异常: {}", e.getMessage(), e);
            throw new ValidationException("校验失败: " + e.getMessage());
        }
    }

    /**
     * 获取Schema（支持缓存和热重载）
     *
     * @return JsonSchema对象
     */
    private JsonSchema getSchema() {

        String schemaLocation = properties.getLocation();

        // 检查是否需要重新加载
        if (needReload(schemaLocation)) {
            log.info("检测到Schema文件变更，重新加载...");
            schemaCache.remove(schemaLocation);
        }

        // 从缓存获取或加载新的Schema
        return schemaCache.computeIfAbsent(schemaLocation, k -> {
            try {
                return loadSchema();
            } catch (Exception e) {
                throw new SchemaLoadException("Schema加载失败", e);
            }
        });
    }

    /**
     * 加载Schema文件
     *
     * @return JsonSchema对象
     * @throws IOException 文件读取异常
     */
    private JsonSchema loadSchema() throws IOException {
        String schemaLocation = properties.getLocation();
        InputStream inputStream = null;

        try {
            // 尝试从主路径加载
            inputStream = loadSchemaStream(schemaLocation);

            if (inputStream == null && properties.getFallbackLocation() != null) {
                // 主路径失败，尝试备用路径
                log.warn("主路径加载失败，尝试备用路径: {}", properties.getFallbackLocation());
                inputStream = loadSchemaStream(properties.getFallbackLocation());
                schemaLocation = properties.getFallbackLocation();
            }

            if (inputStream == null) {
                throw new SchemaLoadException("Schema文件不存在: " + schemaLocation);
            }

            // 解析Schema
            JsonNode schemaNode = objectMapper.readTree(inputStream);
            JsonSchema schema = schemaFactory.getSchema(schemaNode);

            // 记录加载时间
            updateLastModified(schemaLocation);

            log.info("Schema加载成功: {}", schemaLocation);
            return schema;

        } finally {
            if(inputStream != null){
                inputStream.close();
            }
        }
    }

    /**
     * 加载Schema输入流
     *
     * @param location Schema文件位置
     * @return 输入流，加载失败返回null
     */
    private InputStream loadSchemaStream(String location) {
        if (location == null) {
            return null;
        }

        try {
            if (location.startsWith("classpath:")) {
                // 从classpath加载
                String path = location.substring("classpath:".length());
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    log.debug("从classpath加载Schema: {}", path);
                    return resource.getInputStream();
                }
            } else if (location.startsWith("file:")) {
                // 从文件URL加载
                Resource resource = new UrlResource(location);
                if (resource.exists()) {
                    log.debug("从文件URL加载Schema: {}", location);
                    return resource.getInputStream();
                }
            } else {
                // 从绝对路径加载
                File file = new File(location);
                if (file.exists() && file.isFile()) {
                    log.debug("从文件系统加载Schema: {}", location);
                    return new FileInputStream(file);
                }
            }
        } catch (Exception e) {
            log.error("加载Schema失败: {}, 错误: {}", location, e.getMessage());
        }

        return null;
    }

    /**
     * 检查Schema是否需要重新加载
     *
     * @param location Schema位置
     * @return true-需要重载，false-不需要
     */
    private boolean needReload(String location) {

        // 缓存未启用，总是重载
        if (!properties.isCacheEnabled()) {
            return true;
        }

        // 未配置重载间隔，不重载
        if (properties.getReloadInterval() <= 0) {
            return false;
        }

        // 检查文件修改时间
        Long lastModified = schemaLastModified.get(location);
        if (lastModified == null) {
            return true;
        }

        // 获取当前文件修改时间
        long currentModified = getFileLastModified(location);
        return currentModified > lastModified;
    }

    /**
     * 获取文件最后修改时间
     *
     * @param location 文件位置
     * @return 修改时间戳
     */
    private long getFileLastModified(String location) {
        try {
            if (location.startsWith("classpath:")) {
                // classpath资源通常不会变化
                return System.currentTimeMillis();
            } else {
                File file = new File(location);
                if (file.exists()) {
                    return file.lastModified();
                }
            }
        } catch (Exception e) {
            log.debug("获取文件修改时间失败: {}", location);
        }
        return 0L;
    }

    /**
     * 更新文件最后修改时间缓存
     *
     * @param location 文件位置
     */
    private void updateLastModified(String location) {
        long lastModified = getFileLastModified(location);
        schemaLastModified.put(location, lastModified);
    }

    /**
     * 格式化错误消息
     *
     * @param error 校验错误对象
     * @return 格式化后的错误信息
     */
    private String formatErrorMessage(ValidationMessage error) {
        // 示例错误信息：
        // $.orderInfo.cityName: does not have a value in the enumeration [北京, 上海, ...]
        // $.title: must be at least 1 characters long

        String path = error.getPath();
        String message = error.getMessage();

        // 简化路径显示（去掉$前缀）
        if (path.startsWith("$.")) {
            path = path.substring(2);
        } else if (path.equals("$")) {
            path = "根节点";
        }

        return String.format("字段 [%s] 校验失败: %s", path, message);
    }

    /**
     * 清除Schema缓存
     * <p>可通过接口调用，用于强制重新加载Schema</p>
     */
    public void clearCache() {
        log.info("清除Schema缓存");
        schemaCache.clear();
        schemaLastModified.clear();
    }
}
