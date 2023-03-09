# Redis编译问题以及解决方案集锦
```txt
   1. make 执行报错： zmalloc.h:50:10: fatal error: jemalloc/jemalloc.h: No such file or directory
      > 处理方式: make -> make MALLOC=libc
```