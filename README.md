
## app-urlshortener

### 简介

- 练手项目：短链接服务

### 技术栈

- JDK8
- SpringBoot
- MyBatis
- PostgreSQL
- Vue

### 计划实现功能：

- 短链接服务
  - 链接录入
  - 链接查询
  - 链接路由
  - 分布式锁实现（对链接url进行hash处理然后写入数据库的操作支持分布式锁
     - 基于表主键唯一做分布式锁
     - 通过数据库mvcc实现乐观锁
     - 基于Redis实现分布式锁
     - 基于ZooKeeper实现分布式锁
