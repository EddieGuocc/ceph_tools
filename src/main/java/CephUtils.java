import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.text.DateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * @ClassName CephUtils
 * @Description ceph工具类
 * @Author Eddie
 * @Date 2020/08/30 13:32
 */
public class CephUtils extends CephDriver{

    /*
     *@Method getConnection
     *@Description 连接到ceph
     *@Author Eddie
     *@Date 2020/08/30 16:41
     */
    public static AmazonS3 getConnection(){
        ClientConfiguration clientConfig = new ClientConfiguration();
        AWSCredentials credentials = new BasicAWSCredentials(CEPH_PROPERTIES.getAccessKey(), CEPH_PROPERTIES.getSecretKey());
        AmazonS3 s3 = new AmazonS3Client(credentials, clientConfig);
        if (CEPH_PROPERTIES.getEndPoint() != null){
            s3.setEndpoint(CEPH_PROPERTIES.getEndPoint());
        }
        return s3;
    }

    /*
     *@Method getBucketList
     *@Description 获取 Bucket List 多用于测试
     *@Author Eddie
     *@Date 2020/08/30 16:38
     */
    public void getBucketList(){
        AmazonS3 s3 = getConnection();
        s3.listBuckets().forEach(item -> System.out.println(System.currentTimeMillis()/1000 + '\t' +item.getName()));
    }

    /*
     *@Method uploadFile
     *@Params [file, fileNameKey]
     *@Description 上传文件
     *@Author Eddie
     *@Date 2020/08/30 16:40
     */
    public PutObjectResult uploadFile(InputStream file, String fileNameKey){
        AmazonS3 s3 = getConnection();
        return s3.putObject(
                new PutObjectRequest(CEPH_PROPERTIES.getBucketName(), fileNameKey, file, null));
    }

    /*
     *@Method getImage
     *@Params [objectKey]
     *@Description 下载文件并转BASE64格式
     *@Author Eddie
     *@Date 2020/08/30 16:40
     */
    public String getImage(String objectKey) throws IOException {
        AmazonS3 s3 = getConnection();
        S3Object s3Object = s3.getObject(CEPH_PROPERTIES.getBucketName(), objectKey);
        InputStream inputStream = s3Object.getObjectContent();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = inputStream.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    /*
     *@Method createBucket
     *@Params []
     *@Description 创建bucket
     *@Author Eddie
     *@Date 2020/09/08 22:20
     */
    public void createBucket(){
        AmazonS3 s3 = getConnection();
        s3.createBucket("eddie-test-bucket");

    }

    /*
     *@Method deleteObject
     *@Params [key]
     *@Description 根据key 删除文件
     *@Author Eddie
     *@Date 2020/09/08 22:20
     */
    public void deleteObject(String key){
        AmazonS3 s3 = getConnection();
        s3.deleteObject(CEPH_PROPERTIES.getBucketName(), key);
        System.out.println("delete object " + key);
    }

    /*
     *@Method getAll
     *@Params []
     *@Description 返回所有文件列表
     *@Author Eddie
     *@Date 2020/09/08 22:20
     */
    public List<S3ObjectSummary> getAll(){
        AmazonS3 s3 = getConnection();
        ObjectListing listing = s3.listObjects("eddie-test-bucket");
        return listing.getObjectSummaries();
    }

    public void stressTesting(){
        CephUtils cephUtils = new CephUtils();
        // 写线程
        Thread thread1 = new Thread(() -> {
            // 无限循环
            while (true) {
                try {
                    File file1 = new File("C:/Users/Eddie/Desktop/testCeph.jpg");
                    InputStream  inputStream = new FileInputStream(file1);
                    Date date = new Date();
                    DateFormat df = DateFormat.getDateTimeInstance();
                    PutObjectResult result = cephUtils.uploadFile(inputStream, df.format(date));
                    System.out.println(df.format(date) + "\n" + result);
                    Thread.sleep(5000);
                } catch (InterruptedException |FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        // 读线程
        Thread thread2 = new Thread(() -> {
            // 无限循环
            while (true) {
                Date date = new Date();
                DateFormat df = DateFormat.getDateTimeInstance();
                try {
                    System.out.println(df.format(date) + "\n" +cephUtils.getImage("2020-9-6 11:49:49"));
                    Thread.sleep(5000);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread1.start();
        thread2.start();
    }

    public static void main(String[] args) {
        CephUtils cephUtils = new CephUtils();
    }
}
