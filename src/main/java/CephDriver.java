import properties.CephProperties;


import java.util.Properties;

/**
 * @ClassName CephDriver
 * @Description ceph配置文件加载
 * @Author Eddie
 * @Date 2020/08/28 18:26
 */
public class CephDriver {

    //private final static Logger LOGGER = LogManager.getLogger(CephDriver.class);
    protected final static CephProperties CEPH_PROPERTIES = new CephProperties();


    static {
        System.out.println("加载ceph配置信息...");
        Properties properties = new Properties();
        try {
            properties.load(CephDriver.class.getResourceAsStream("/ceph.properties"));
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
        CEPH_PROPERTIES.setAccessKey(properties.getProperty("ceph.accessKey"));
        CEPH_PROPERTIES.setSecretKey(properties.getProperty("ceph.secretKey"));
        CEPH_PROPERTIES.setEndPoint(properties.getProperty("ceph.endPoint"));
        CEPH_PROPERTIES.setBucketName(properties.getProperty("ceph.bucketName"));

        System.out.println("配置信息加载完毕[ endPoint=" + CEPH_PROPERTIES.getEndPoint() + ", accessKey = ***, secretKey = ***, bucketName = "
                + CEPH_PROPERTIES.getBucketName() + "]");
    }
}
