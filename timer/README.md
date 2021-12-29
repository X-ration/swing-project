# timer

------------
基于Swing开发的计时器，具备统计功能，Windows10&Ubuntu18测试通过<br>

## v1.2版本
结束程序时可记录状态，在下一次启动时可恢复到先前的状态。

### 1.2.4版本
重构计时器代码；基于ActionLog的统计功能；暂时去除统计功能开关<br>
![](readme/v1.2/1.2.4-stat.png)
### 1.2.3版本
优化代码，通过loader构建jar包
### 1.2.2版本
统计功能开关<br>
![](readme/v1.2/stat-option.png)
### 1.2.1版本
增加了删除按钮<br>
![](readme/v1.2/add-delete-button.png)

## v1.1版本
支持多个计时器同时运行；优化了计时代码。<br>
![](readme/v1.1/multiple-timers.png)

## v1.0版本
- 支持设置一个计时器，最大计时24小时，最小粒度支持到分钟
- 开始计时后，展示“时分秒”格式的剩余时间
- 计时完成时，向通知中心发出通知并播放通知声音
- 支持暂停和重置功能
- 支持后台运行，可收起到任务栏托盘图标
- 支持每日汇总统计<br>
  ![](readme/v1.0/counting.png)
  ![](readme/v1.0/statistic.png)
  ![](readme/v1.0/notice.png)