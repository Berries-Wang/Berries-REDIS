#!/bin/bash
echo 'Redis Sentinel 26379 启动'
if [ $# -ne 1 ];then
    echo "请指定参数"
    exit 1;
fi
PATHDIR=`pwd`
echo "当前目录:`pwd`"
if [[ $1 = "I" ]];then
    rm -rf ./work_dir_26379
    cp sentinel_26379.conf  sentinel_26379_copy.conf
    mkdir work_dir_26379
elif [[ $1 = "A" ]];then
    echo "重新上线..."
else
    echo "未知选项..."
    exit 1;
fi

"./../../../..//001.REDIS_SOURCE_CODE/redis-6.2.5/src/redis-server" ${PATHDIR}/sentinel_26379_copy.conf --sentinel