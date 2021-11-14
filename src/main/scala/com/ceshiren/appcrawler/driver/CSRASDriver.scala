package com.ceshiren.appcrawler.driver

import com.ceshiren.appcrawler._
import com.ceshiren.appcrawler.core.CrawlerConf
import com.ceshiren.appcrawler.model.URIElement
import com.ceshiren.appcrawler.utils.Log.log
import com.ceshiren.appcrawler.utils.DynamicEval
import org.openqa.selenium.Rectangle

import java.awt.{BasicStroke, Color}
import java.io.File
import javax.imageio.ImageIO
import scala.sys.process._

/**
 * Created by seveniruby on 18/10/31.
 */
class CSRASDriver extends ReactWebDriver {
  DynamicEval.init()
  var conf: CrawlerConf = _
  val adb = getAdb()
  val session = requests.Session()

  //csras本地映射的地址
  val csrasUrl = "http://127.0.0.1:7778"
  var packageName = ""
  var activityName = ""

  def this(url: String = "http://127.0.0.1:4723/wd/hub", configMap: Map[String, Any] = Map[String, Any]()) {
    this

    log.info(s"url=${url}")


    val apkPath = shell(s"${adb} shell pm list packages")
    if (apkPath.indexOf("com.hogwarts.csruiautomatorserver") == -1) {
      log.info("No Driver Apk In Device,Need Install！")
      val path = System.getProperty("user.dir")
      log.info(s"DIR=${path}")
      //安装apk
      shell(s"${adb} install ${path}/app-debug.apk")
    }
    // 给CSRAS驱动设置权限，使驱动可以自行开启辅助功能
    shell(s"${adb} shell pm grant com.hogwarts.csruiautomatorserver android.permission.WRITE_SECURE_SETTINGS")
    // 启动CSRAS，加载辅助服务
    shell(s"${adb} shell am start com.hogwarts.csruiautomatorserver/com.hogwarts.csruiautomatorserver.MainActivity")

    packageName = configMap.getOrElse("appPackage", "").toString
    activityName = configMap.getOrElse("appActivity", "").toString

    //设置端口转发，将csras的端口映射到本地，方便访问
    shell(s"${adb} forward tcp:7778 tcp:7777")
    // todo:将等待改为通过轮询接口判断设备上的服务是否启动
    Thread.sleep(3000)
    //设置包过滤参数，使用yaml文件中配置的packageName进行包过滤，避免系统事件污染pageSource
    setPackageFilter()
    if (configMap.getOrElse("noReset", "").toString.equals("false")) {
      shell(s"${adb} shell pm clear ${packageName}")
    } else {
      log.info("need need to reset app")
    }

    if (packageName.nonEmpty) {
      shell(s"${adb} shell am start -W -n ${packageName}/${activityName}")
    }
  }

  def setPackageFilter(): Unit ={
    //通过发送请求，设置关注的包名，过滤掉多余的数据
    log.info(s"set package ${packageName}")
    session.get(s"${csrasUrl}/setPackage?package=${packageName}")
  }

  override def event(keycode: String): Unit = {
    shell(s"${adb} shell input keyevent ${keycode}")
    log.info(s"event=${keycode}")
  }

  //todo: outside of Raster 问题
  override def getDeviceInfo(): Unit = {
    val size = shell(s"${adb} shell wm size").split(' ').last.split('x')
    screenHeight = size.last.trim.toInt
    screenWidth = size.head.trim.toInt
    log.info(s"screenWidth=${screenWidth} screenHeight=${screenHeight}")
  }

  override def swipe(startX: Double = 0.9, startY: Double = 0.1, endX: Double = 0.9, endY: Double = 0.1): Unit = {
    val xStart = startX * screenWidth
    val xEnd = endX * screenWidth
    val yStart = startY * screenHeight
    val yEnd = endY * screenHeight
    log.info(s"swipe screen from (${xStart},${yStart}) to (${xEnd},${yEnd})")
    shell(s"${adb} shell input swipe ${xStart} ${yStart} ${xEnd} ${yEnd}")
  }


  override def screenshot(): File = {
    val file = File.createTempFile("tmp", ".png")
    log.info(file.getAbsolutePath)
    val cmd = s"${adb} exec-out screencap -p"
    log.info(cmd)
    (cmd #> file).!!
    file
  }

  //todo: 重构到独立的trait中
  override def mark(fileName: String, newImageName: String, x: Int, y: Int, w: Int, h: Int): Unit = {
    val file = new java.io.File(fileName)
    log.info(s"read from ${fileName}")
    val img = ImageIO.read(file)
    val graph = img.createGraphics()

    if (img.getWidth > screenWidth) {
      log.info("scale the origin image")
      graph.drawImage(img, 0, 0, screenWidth, screenHeight, null)
    }
    graph.setStroke(new BasicStroke(5))
    graph.setColor(Color.RED)
    graph.drawRect(x, y, w, h)
    graph.dispose()

    log.info(s"write png ${fileName}")
    if (img.getWidth > screenWidth) {
      log.info("scale the origin image and save")
      //fixed: RasterFormatException: (y + height) is outside of Raster 横屏需要处理异常
      val subImg = tryAndCatch(img.getSubimage(0, 0, screenWidth, screenHeight)) match {
        case Some(value) => value
        case None => {
          getDeviceInfo()
          img.getSubimage(0, 0, screenWidth, screenHeight)
        }
      }
      ImageIO.write(subImg, "png", new java.io.File(newImageName))
    } else {
      log.info(s"ImageIO.write newImageName ${newImageName}")
      ImageIO.write(img, "png", new java.io.File(newImageName))
    }
  }

  override def click(): this.type = {
    val center = currentURIElement.center()
    shell(s"${adb} shell input tap ${center.x} ${center.y}")
    this
  }

  override def tap(): this.type = {
    click()
  }

  override def tapLocation(x: Int, y: Int): this.type = {
    val pointX = x * screenWidth
    val pointY = y * screenHeight
    shell(s"${adb} shell input tap ${pointX} ${pointY}")
    this
  }

  override def longTap(): this.type = {
    val center = currentURIElement.center()
    log.info(s"longTap element in (${center.x},${center.y})")
    shell(s"${adb} shell input swipe ${center.x} ${center.y} ${center.x + 0.1} ${center.y + 0.1} 2000")
    this
  }

  override def back(): Unit = {
    shell(s"${adb} shell input keyevent 4")
  }

  override def backApp(): Unit = {
    shell(s"${adb} shell am start -W -n ${packageName}/${activityName}")
  }

  override def getPageSource(): String = {
    session.get(s"${csrasUrl}/source").text()
  }

  override def getAppName(): String = {
    session.get(s"${csrasUrl}/fullName").text().split('/').head
  }

  override def getUrl(): String = {
    session.get(s"${csrasUrl}/fullName").text().split('/').last.stripLineEnd
  }

  override def getRect(): Rectangle = {
    //selenium下还没有正确的赋值，只能通过api获取
    if (currentURIElement.getHeight != 0) {
      //log.info(s"location=${location} size=${size} x=${currentURIElement.x} y=${currentURIElement.y} width=${currentURIElement.width} height=${currentURIElement.height}" )
      new Rectangle(currentURIElement.getX, currentURIElement.getY, currentURIElement.getHeight, currentURIElement.getWidth)
    } else {
      log.error("rect height < 0")
      return null
    }
  }

  override def sendKeys(content: String): Unit = {
    tap()
    shell(s"${adb} shell input text ${content}")
  }

  override def launchApp(): Unit = {
    //driver.get(capabilities.getCapability("app").toString)
    back()
  }

  override def findElementsByURI(element: URIElement, findBy: String): List[AnyRef] = {
    List(element)
  }

  override def reStartDriver(): Unit = {
    shell(s"${adb} shell am force-stop com.hogwarts.csruiautomatorserver")
    shell(s"${adb} shell am start com.hogwarts.csruiautomatorserver/com.hogwarts.csruiautomatorserver.MainActivity")
    setPackageFilter()
  }

  def getAdb(): String = {
    List(System.getenv("ANDROID_HOME"), "platform-tools/adb").mkString(File.separator)
  }

  def shell(cmd: String): String = {
    log.info(cmd)
    val result = cmd.!!
    log.info(result)
    result
  }

}

