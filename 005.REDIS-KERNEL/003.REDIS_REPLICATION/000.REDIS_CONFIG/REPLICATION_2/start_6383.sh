#!/bin/bash
echo 'Redis Server 启动'
PATHDIR=`pwd`
echo "当前目录:`pwd`" 
"./../../../..//001.REDIS_SOURCE_CODE/redis-6.2.5/src/redis-server" ${PATHDIR}/replica_6383.conf