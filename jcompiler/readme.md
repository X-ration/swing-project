## 基于Swing的Java编译器
- 1.0版本 基础功能<br>
  可指定源代码目录、输出目录执行编译<br>
  支持Linux/Windows<br>
  本地测试通过<br>
  ![](readme/v1.0/compile.png)
- 1.1版本 支持项目布局<br>
  可使用默认布局，即<br>
  --            根目录<br>
  ----src       源代码<br>
  ----resources 资源目录<br>
  使源代码和资源目录一同构建。<br>
  ![](readme/v1.1/build_windows.png)