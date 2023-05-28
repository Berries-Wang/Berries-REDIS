#!/bin/bash
echo 'Redis Server 启动'
if [ $# -ne 1 ];then
    echo "请指定参数"
    exit 1;
fi
PATHDIR=`pwd`
echo "当前目录:`pwd`"
if [[ $1 = "I" ]];then
    echo "第一次启动..."
    cp master.conf master_copy.conf
    rm -f ./*.aof && rm -f ./*.rdb
elif [[ $1 = "A" ]];then
    echo "重新上线..."
else
    echo "未知选项..."
    exit 1;
fi

"./../../../../001.REDIS_SOURCE_CODE/redis-6.2.5/src/redis-server" ${PATHDIR}/master_copy.conf