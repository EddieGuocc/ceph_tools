##准备工作##

###以下操作全在root用户下执行###
###系统版本 Ubuntu 18.04.4 LTS###
###内核版本4.15.0-122-generic###

1. 修改时区

	 	timedatectl set-timezone Asia/Shanghai

2. 各个节点同步时间

		 apt install -y ntp ntpdate	
		 systemctl stop ntp
		 ntpdate ntp.aliyun.com
	
	 #修改ntp配置文件#

		sed -i 's/pool/#pool/' /etc/ntp.conf
		cat >> /etc/ntp.conf<<EOF
		server ntp.aliyun.com iburst
		server ntp1.aliyun.com iburst
		server ntp2.aliyun.com iburst
		server ntp3.aliyun.com iburst
		EOF

	 #启动#

		systemctl enable ntp
		systemctl start ntp

3. **[基于实际配置修改]**修改/etc/hosts 添加ceph多个节点的解析
   例: 

		192.168.1.191 ceph1
		192.168.1.192 ceph2
		192.168.1.193 ceph3

4. **[主节点操作]**为ceph相关指令提供python环境

		apt install -y python-minimal

5. **[主节点操作]**生成ssh免密钥root权限登录
	
		ssh-keygen
		ssh-copy-id root@ceph1
		ssh-copy-id root@ceph2
		ssh-copy-id root@ceph3


##ceph安装##
1. 添加ceph源(官方源)

		wget --no-check-certificate -q -O- 'https://download.ceph.com/keys/release.asc' | apt-key add -
	
		echo deb https://download.ceph.com/debian-luminous/ $(lsb_release -sc) main | sudo tee /etc/apt/sources.list.d/ceph.list
	  
2. 更换ceph源(163源)

		sed -i 's#https://download.ceph.com#http://mirrors.163.com/ceph#' /etc/apt/sources.list.d/ceph.list

3. 更新源

		apt update

4. **[主节点操作]**创建集群文件夹，所有关于ceph的操作都要进入此文件夹下

		mkdir /usr/local/ceph-cluster
		cd /usr/local/ceph-cluster

5. **[主节点操作]**安装ceph部署工具

		apt install ceph-deploy -y

6. **[主节点操作]**主节点创建集群，默认集群名字为ceph，此节点名字为ceph1，此时集群目录下会出现配置文件

		ceph-deploy new ceph1

7. **[主节点操作]**主节点向各个节点安装ceph，**安装前一定要注意软件园已经更换为2中的163源**

		ceph-deploy install --no-adjust-repos ceph1 ceph2 ceph3

8. **[基于实际配置修改]**ceph配置文件修改，添加public_network属性

		echo "public_network = 192.168.1.0/24">> ceph.conf

9. **[主节点操作]**初始化Monitor，主要用作维护集群健康状态，数目为单数个

		ceph-deploy mon create-initial

10. **[主节点操作]**推送admin.keyring到各个子节点，执行后会在子节点/etc/ceph文件夹下看到配置文件

		ceph-deploy admin ceph1 ceph2 ceph3

11. **[主节点操作]**各节点同步并创建Monitor

		ceph-deploy mon create ceph1 ceph2 ceph3

		查看quorum，执行以下命令后，会出现所有节点的相关配置json

		ceph quorum_status --format json-pretty

			{
		    "election_epoch": 14,
		    "quorum": [
		        0,
		        1,
		        2
		    ],
		    "quorum_names": [
		        "ceph3",
		        "ceph1",
		        "ceph2"
		    ],
		    "quorum_leader_name": "ceph3",
		    "monmap": {
		        "epoch": 3,
		        "fsid": "2531cfea-e513-4bd2-b752-e0a03be170b3",
		        "modified": "2020-10-28 15:11:19.913673",
		        "created": "2020-10-28 14:58:06.548884",
		        "features": {
		            "persistent": [
		                "kraken",
		                "luminous",
		                "mimic",
		                "osdmap-prune"
		            ],
		            "optional": []
		        },
		        "mons": [
		            {
		                "rank": 0,
		                "name": "ceph3",
		                "addr": "192.168.1.123:6789/0",
		                "public_addr": "192.168.1.123:6789/0"
		            },
		            {
		                "rank": 1,
		                "name": "ceph1",
		                "addr": "192.168.1.191:6789/0",
		                "public_addr": "192.168.1.191:6789/0"
		            },
		            {
		                "rank": 2,
		                "name": "ceph2",
		                "addr": "192.168.1.205:6789/0",
		                "public_addr": "192.168.1.205:6789/0"
		            }
		        ]
		    }
		}


12. **[主节点操作]**创建MGR
	
		ceph-deploy mgr create ceph1 ceph2 ceph3

13. **[主节点操作]**启用MGR

		ceph mgr module enable dashboard

14. **[主节点操作]**配置OSD，OSD主要负责存储数据，处理数据的恢复，复制，回补，数据平衡。 并提供数据给Monitor，在做此操作之前，执行 

		ceph -s

    查看当前集群状态命令，显示OSD数量太少。

		cluster:
	    id:     2531cfea-e513-4bd2-b752-e0a03be170b3
	    health: HEALTH_WARN
	            OSD count 0 < osd_pool_default_size 3

15. **[主节点操作]****[基于实际配置修改]**创建OSD，此处建议每个节点创建3个OSD（3块磁盘），每块磁盘分区大小20GB+，测试环境10GB空间会产生bug，导致一段时间内OSD空间变满。

		ceph-deploy osd create ceph1 --data /dev/sdb
		ceph-deploy osd create ceph1 --data /dev/sdc
		ceph-deploy osd create ceph1 --data /dev/sdd
		ceph-deploy osd create ceph2 --data /dev/sdb
		ceph-deploy osd create ceph2 --data /dev/sdc
		ceph-deploy osd create ceph2 --data /dev/sdd
		ceph-deploy osd create ceph3 --data /dev/sdb
		ceph-deploy osd create ceph3 --data /dev/sdc
		ceph-deploy osd create ceph3 --data /dev/sdd

	执行以上操作之后再查看当前集群状态，显示状态OK

		cluster:
	    id:     2531cfea-e513-4bd2-b752-e0a03be170b3
	    health: HEALTH_OK

16. **[主节点操作]****[基于实际配置修改]**创建pool
	1. 首先计算pool的pg_num和pgp_num,基于当前环境OSD数量进行配置
		
			OSD少于5     pg_num设为128
			OSD介于5-10  pg_num设为512
			OSD介于10-50 pg_num设为4096
			pg_num = (OSD_NUM * 100) / 副本数 向上取最接近的2的N次幂
			pgp_num = (OSD_NUM * 100) / 副本数 / 池数 向上取最接近的2的N次幂

	2. 创建pool
	
			执行命令设置，pool名ytsdwan，pg_num 和 pgp_num 都为512
			ceph osd pool create ytsdwan 512 512

17. **[主节点操作]****[基于实际配置修改]**创建用户

		radosgw-admin user create --uid="yingtongkeji@dalian" --display-name="yingtongkeji@dalian"

		执行完毕后会返回一段该用户的json数据

		{
		    "user_id": "yingtongkeji@dalian",
		    "display_name": "yingtongkeji@dalian",
		    "email": "",
		    "suspended": 0,
		    "max_buckets": 1000,
		    "auid": 0,
		    "subusers": [],
		    "keys": [
		        {
		            "user": "yingtongkeji@dalian",
		            "access_key": "T291DM2MODEX7QE2I1AF",						#S3颁发，20字符，标识身份
		            "secret_key": "wu5Sm0E2NpMgSEdGvZTIdOQTPAEWsFYXZtZ5O8An"	#S3颁发，40字符，私钥
		        }
		    ],
		    "swift_keys": [],
		    "caps": [],
		    "op_mask": "read, write, delete",
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
		    "type": "rgw",
		    "mfa_ids": []
		}


		其中 **access_key** 和 **secret_key** 需要记录，是其他程序通过S3客户端访问ceph进行读写操作的密钥

18. 添加RGW，是ceph的网关组件，默认为7480端口，S3客户端与ceph交互都走此网关

		ceph-deploy rgw create ceph1 ceph2 ceph3

		访问http://192.168.1.191:7480/ 出现xml证明配置成功，可以使用S3客户端进行访问
		若想修改默认端口，修改ceph.conf配置文件，添加属性
		rgw_frontends = "civetweb port=2345"
	
		推送配置文件到需要的节点
		ceph-deploy --overwrite-conf config push ceph1 ceph2 ceph3
		更新的节点重启rgw
		systemctl restart ceph-radosgw@rgw.ceph3.service
	
