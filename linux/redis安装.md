# 安装redis

# 下载Redis

```html
 wget http://download.redis.io/releases/redis-5.0.5.tar.gz
```

解压

```shell
tar -zxvf redis-5.0.5.tar.gz
#移动文件
mv redis-5.0.5.tar.gz /usr/redis
//进入redis目录
cd /usr/redis 
#安装gcc环境
 yum -y install gcc-c++
#make编译
make
#进入src目录
cd src/
#编译
make install
#新建文件夹
mkdir ect
mkdir bin

```

