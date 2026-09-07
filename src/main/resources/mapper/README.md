### 数据访问层 (DAO层)。负责与数据库进行交互，执行SQL语句。结合你之前问的MyBatis，
### 这里就是放置Mapper接口的地方，接口上会使用 @Mapper 注解。
### MyBatis的SQL映射文件（.xml）通常放在 src/main/resources/mapper/ 目录下。