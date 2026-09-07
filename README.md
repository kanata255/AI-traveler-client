com.example.demo/
├── DemoApplication.java          # 启动类
├── controller/                   # 控制层：接收HTTP请求，调用Service
│   └── UserController.java
├── service/                      # 业务逻辑层：核心业务处理
│   ├── UserService.java          # 接口
│   └── impl/
│       └── UserServiceImpl.java  # 实现类（以Impl结尾）
├── mapper/                       # 数据访问层（DAO层）：数据库交互
│   └── UserMapper.java
├── model/ (或 entity/)           # 实体类：与数据库表对应
│   └── User.java
├── dto/                          # 数据传输对象：接口层数据载体
│   └── UserDTO.java
├── config/                       # 配置类：替代或补充application.yml
│   └── WebConfig.java
└── common/ (或 util/)            # 公共组件：工具类、常量、统一响应、异常定义
├── Result.java               # 统一返回结果封装
└── GlobalExceptionHandler.java
