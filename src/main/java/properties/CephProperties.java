package properties;

import lombok.Getter;
import lombok.Setter;

/**
 * @ClassName CephpProperties
 * @Description 配置文件类
 * @Author Eddie
 * @Date 2020/08/28 18:13
 */
@Getter
@Setter
public class CephProperties {

    private String accessKey;

    private String secretKey;

    private String endPoint;

    private String bucketName;

}
