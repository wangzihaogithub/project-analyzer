# project-analyzer (帮助分析项目中的代码统计)

### 简介

- 支持统计对外暴露的Dubbo接口数量及详细
- 支持统计对外暴露的Http接口数量及详细
- 支持统计依赖调用内部接口数量及详细
- 支持统计依赖调用外部接口数量及详细
- 支持统计依赖调用调用其他接口数量及详细

github地址 : https://github.com/wangzihaogithub/project-analyzer

### 使用方法 - 在test目录debug执行查看即可

1. 将jar包放入 /test/resources目录下
2. debug执行 /test/java/ClassProjectStatTest的main方法
3. 在debug中查看groupBy字段的统计结果
