# fat-jar-loader

------------
适配Fat-jar需求的loader项目<br>

### v1.0版本
将应用jar包放到lib目录并在MANIFEST文件中指定Fat-Jar-Run-Class-Name=<应用启动类名>即可运行<br>
定位Class、Resource均为嵌套查找，即可以识别Fat-Jar中的Fat-Jar的Class、Resource
