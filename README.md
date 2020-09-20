# ceph_tools
A project for base ceph operation using java

###基本概念
ceph核心组件
1. OSD(Object Storage Device) 
    
    主要负责存储数据，处理数据的恢复，复制，回补，数据平衡。
    并提供数据给Monitor，一个能正常工作的集群至少要两个OSD(双副本)。
    
    以下以3节点、9OSD为例子
    
    节点1 OSD [0,1,2]
    
    节点2 OSD [3,4,5]
    
    节点3 OSD [6,7,8]
    
    实际储存的时候会有主从副本之分，主从副本一定是分散在不同节点上,比如[1,5,8],[0,3,7]。当出现故障等情况时，
    从OSD可能被提升为主OSD，同时新的OSD从副本将新生成。
    
    查看当前主从副本OSD分布情况
    
        ceph pg ls-by-pool <poolname> #查看pool下所有主从副本列表
        ceph pg ls-by-osd osd.1 #查看包括此OSD的所有主从副本列表
        ceph pg ls-by-primary osd.1 #查看以此OSD为主副本的列表
        
    一个新的文件存入ceph的过程：
    1. file进入，进行分片，例如4M每个，拆分成多个object，每个object由file的元数据(元数据指数据的属性，位
    置，大小，储存时间) + ceph条带化生成的Object的序列号组成。  --得到oid(hash值)
    2. 根据oid和mark(mark 为集群中 PG总数 - 1)计算出pgid
    3. Crush算法接受pgid，返回包含n个OSD的集合，诸如[2,5,7]
    其中pg(placement group)属于一个逻辑概念，添加在储存对象和OSD之间，与OSD是映射关系(逻辑 -> 物理)，是一
    个对象的合集，按照上述iii中的结合结果，此时该pg内的所有Object都会在osd.2,osd.5,osd.7中保存3分副本，而
    这三个osd也承担了共同存储和维护Object的任务,2为主，5，7为从。
    
2. Monitor

    监控器，主要负责维护集群的健康状态，提供一致性的决策，集群推荐monitor的数量为奇数，某一个Leader Monitor
    节点宕机后，能推举出另一个节点成为Leader。包含Monitor Map、OSD Map、
    PG Map、CRUSH Map。
    
3. MDS(Metadata Server)
    
    保存CEPH文件系统的元数据和文件，块存储和对象存储使用不到。
    对象存储RADOWS(Reliable, Autonomic, Distributed, Object Storage Gateway)
    块存储RBD(Rados Block Device)
    文件存储FS(File System)
    
    本项目使用的就是对象储存，使用S3 SDK访问，RGW(RADOS Gateway)——基于LIBRADOS提供的RESTful风格接口。
    基于HTTP标准，适用于Web类互联网程序，实现文件上传下载等操作。
    
    层级
    
        RADOSGW[S3, Swift...]   #RESTful网关，可理解为LIBRADOS又一层封装
                ↓
             LIBRADOS           #操作RADOS的接口，支持多种语言直接访问
                ↓
        RADOS[OSD, Monitor]     #底层核心，分布式存储       
        
4. 常用
    
        root@guo:/usr/local/ceph-cluster#rados lspools #查看ceph中所有的pool
        .rgw.root                   #region 和 zone配置信息
        default.rgw.control
        default.rgw.meta
        default.rgw.log
        ytsdwan
        default.rgw.buckets.index   #储存每个bucket在rados里面的索引信息
        default.rgw.buckets.data    #按照索引存储每个bucket下的对象
        default.rgw.buckets.non-ec
        
        root@guo:/usr/local/ceph-cluster# radosgw-admin user info --uid=eddie #查看用户信息
        {
            "user_id": "eddie",
            "display_name": "eddie",
            "email": "",
            "suspended": 0,
            "max_buckets": 1000,                           #RGW中每个用户最多创建1000个bucket
            "auid": 0,
            "subusers": [],
            "keys": [
                {
                    "user": "eddie",
                    "access_key": "XXXXXXXXXXXXXX",        #S3颁发，20字符，标识身份
                    "secret_key": "xxxxxxxxxxxxxxxxxxxxx"  #S3颁发，40字符，私钥
                }
            ],
            "swift_keys": [],
            "caps": [],
            "op_mask": "read, write, delete",              #权限
            "default_placement": "",
            "placement_tags": [],
            "bucket_quota": {
                "enabled": false,
                "check_on_raw": false,
                "max_size": -1,
                "max_size_kb": 0,
                "max_objects": -1
            },
            "user_quota": {
                "enabled": false,
                "check_on_raw": false,
                "max_size": -1,
                "max_size_kb": 0,
                "max_objects": -1
            },
            "temp_url_keys": [],
            "type": "rgw"
        }

    
    
    
