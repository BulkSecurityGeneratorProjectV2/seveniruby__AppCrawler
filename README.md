## 配置参数说明

Appcrawler是一个基于自动遍历的App爬虫工具，支持Android和IOS，支持真机和模拟器。最大的特点是灵活性高，可通过配置来设定遍历的规则

### 启动Appcrawler

```bash
Usage: java -jar appcrawler.jar [options]

-a <value> : --app，Android或者iOS的文件地址，可以是网络地址，赋值给appium的app选项
示例 : java -jar appcrawler.jar -a APIDemos.apk

-c <value> : --conf，配置文件的地址，支持YAML和JSON格式，不指定即使用默认配置
示例 : java -jar appcrawler.jar -c APIDemosConfig.yml

-e <value> : --encoding，设置编码格式
示例 : java -jar appcrawler.jar -a APIDemos.apk -e UTF-8/GBK

-p <value> : --platform，平台类型即Android或者IOS，若不指定会根据App后缀名自动判断
示例 : java -jar appcrawler.jar -a APIDemos.apk -p Android/IOS

-t <value> : --maxTime，最大运行时间，单位为秒，超过此值会退出，默认最长运行3个小时
示例 : java -jar appcrawler.jar -a APIDemos.apk -t 10800

-u <value> : --appium，appium的url地址
示例 : java -jar appcrawler.jar -a APIDemos.apk -u http://127.0.0.1:4730/wd/hub

-o <value> : --output，遍历结果的保存目录，里面会存放遍历生成的截图，思维导图和日志
示例 : java -jar appcrawler.jar -a APIDemos.apk -o result/

--capability k1=v1,k2=v2... : 使用这个参数会覆盖-c指定的配置模板里的参数，用于模板配置里的capability参数微调
示例 : java -jar appcrawler.jar --capability "appPackage=com.android.xx,appActivity=.view.xxActivity"

-r <value> : --report，输出html和xml报告的目录
示例 : java -jar appcrawler.jar -a APIDemos.apk -r result/report/

-y <value> : --yaml，代表配置的yaml语法，用于不使用配置文件的情况下添加约束
示例 : java -jar appcrawler.jar -a APIDemos.apk -y "blackList: [ {xpath: action_night } ]"

--template <value> : 输出代码模板
示例 : java -jar appcrawler.jar -a APIDemos.apk --template "xx"

--master <value> : master的elements.yml文件地址，与--candidate参数一起使用，生成两者的Diff报告

--candidate <value> : candidate环境的elements.yml文件，同上
示例 : java -jar appcrawler.jar --master master.yml --candidate candidate.yml -r result/diff/

-v : --verbose-debug，是否展示更多debug信息
示例 : java -jar appcrawler.jar -a APIDemos.apk -v

-vv : --verbose-trace，是否展示更多trace信息
示例 : java -jar appcrawler.jar -a APIDemos.apk -vv

--demo : 生成demo配置文件的示例
示例 : java -jar appcrawler.jar --demo

--help : 输出帮助文档
示例 : java -jar appcrawler.jar --help
```

### 配置文件格式

**以YAML格式为例，以下为默认配置 :**

```
---
pluginList:             /** 插件列表，暂时禁用 */
useNewData: false       /** 是否使用新数据格式 */
logLevel: "TRACE"       /** 设置log的层级 */
saveScreen: true        /** 是否截图 */
showCancel: true        /** 是否展示取消的操作 */
reportTitle: ""         /** 结果目录 */
beforeStartWait: 6000   /** 等待启动，待废除 */
maxTime: 10800          /** 最大运行时间 */
maxDepth: 10            /** 默认的最大深度10, 结合baseUrl可很好的控制遍历的范围 */
resultDir: ""           /** 结果目录 */
findBy: "xpath"         /** 可选 default|android|id|xpath，默认状态会自动判断是否使用android定位或者ios定位 */
capability:             /** appium的capability通用配置，需要指定appPackage和appActivity */
  noReset: "true"
  fullReset: "false"
baseUrl:                /** 设置一个起始url，指定遍历的初始状态 */
urlBlackList:           /** url黑名单，用于排除某些页面 */
blackList:              /** 黑名单列表 matches风格, 默认排除内容是2个数字以上的控件. */
- xpath: ".*[0-9]{2}.*"
urlWhiteList:           /** url白名单 */
appWhiteList:           /** app白名单 */
backButton:             /** 后退按钮标记，目前具备了自动判断返回按钮的能力 */
- xpath: "Navigate up"
firstList:              /** 基于selectedList定位到的元素，优先遍历的元素列表 */
selectedList:           /** 使用xpath定位期望的遍历列表 */
- xpath: "//*[contains(name(), 'Button')]"
- xpath: "//*[@clickable="true"]//android.widget.TextView[string-length(@text)>0 and string-length(@text)<20]"
lastList:               /** 基于selectedList定位到的元素，最后遍历的元素列表 */
- xpath: "//*[contains(@resource-id, 'header')]//*"
- xpath: "//*[contains(@resource-id, 'indicator')]//*"
beforeRestart：         /** 在重启session之前做的事情 */
beforeElement:          /** 在执行action之前默认执行的动作，比如等待 */
afterElement:           /** 在执行action之后默认执行的动作，比如等待 */
afterElementWait： 500  /** 执行action之后的等待时间 */
afterAll:               /** 所有动作执行完后，是否需要刷新或者滑动 */
afterAllMax：           /** afterAll执行多少次后才不执行，比如连续滑动2次都没新元素即取消 */
triggerActions:         /** 引导规则，action动作，xpath定位，times执行次数 */
- xpath: "permission_allow_button"
  times: 3
- xpath: "允许"
  times: 3
tagLimitMax: 2          /** 相似控件最多点击几次 */
tagLimit:               /** 特殊的按钮，可以一直被遍历 */
- xpath: "确定"
  times: 1000
testcase:               /** 可以自定义测试用例 */
  name: TesterHome AppCrawler
  steps:
  - when:
      xpath: "/*/*"
      action: "Thread.sleep(1000)"
    then: ""
xpathAttributes：       /** 自动生成的xpath表达式里可以包含的匹配属性 */
- "name()"
- "resource-id"
sortByAttribute:        /** 先按照深度depth排序，再按照list排序，最后按照selected排序。后排序是优先级别最高的 */
- "depth"
- "list"
- "selected"
suiteName:              /** 用来确定url的元素定位xpath 他的text会被取出当作url因素 */
- "//*[@selected='true']//android.widget.TextView/@text"
assertGlobal:           /** 断言，只需要写given与then即可 */
```

## 编译

```bash
mvn clean package assembly:single
```