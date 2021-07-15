# 编写Xposed模块
了解完Xposed框架的相关知识后，我们还要编写一些模块代码，才能实现我们的监测操作。
首先在gradle里面依赖一下xposed的api：
```go
compileOnly 'de.robv.android.xposed:api:82'
compileOnly 'de.robv.android.xposed:api:82:sources'
```
在进行Xposed模块开发之前，我们有必要了解一下[Xposed API](https://api.xposed.info/reference/packages.html)。完成一个模块的开发至少有两步要做:
```md
1、编写一个java类并实现**IXposedHookLoadPackage**接口，实现**handleLoadPackage**方法进行自定义的监测操作
2、注册这个java类
```
## 编写代码
假如我们需要监测的方法是:
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714135110971.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
那么，我们的初始方法就可以写成这个样子:
```java
public class HookTrack implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

    }
}
```
在**handleLoadPackage**中，调用**XposedHelpers**类的**findMethodHook**来进行，在写代码的时候，我们发现其实有两个方式可以选用:
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714140941115.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
区别在于第一个方法传入的是class本体，然后源码那边使用的classLoader就是**class.getClassLoader**;第二种不需要class本体，只需要指定这个class的名字，然后再指定加载这个class的classLoader。从便捷上来说，第一种无疑是便捷的。但是第二种的灵活度比第一种高。假如有一些类是第三方SDK里面的，而这个SDK没在你源码里面，是以插件形式在你app安装完后才加进来的。这时候，你在编码阶段是没有办法得到这个class本体的，所以第二种方法可以看作是能hook运行时的class，并且官方注释还给出了第二种的使用模式:
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714141740638.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
因此，按照官方提供的思路，我们可以这样写:
```java
XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getDeviceId",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getDeviceId()获取了imei");
                    }
                }
        );
```
注意到我们最后的那个回调函数**XC_methodHook**,
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714142611321.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
首先，这是一个抽象类，不是接口。**beforeHookMethod**和**afterHookMethod**从字面意思也能看出是在hook前后的调用回调。然后其构造函数有两个，有一个是带int类型的，传入的是一个设置hook优先级的数字。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714142827322.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
从方法注释上看，这个priority会影响后面**beforeHookMethod**和**afterHookMethod**的调用顺序。优先级越高的Hook,其beforeHook方法会越先执行，然后其afterHook方法会在最后执行。如果存在hook多个方法，且所有的priority都相同，会依次此执行完这个方法的before和after在执行下一个方法的before和after，以此类推。
而采用无参构造的，其priority是一个系统默认值50:
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714143342196.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
假如我们Hook了3个方法A,B,C。在priority相同和不同时的调用关系可以参考下图:

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714150116541.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
知道了上面的原理后，我们就应该选用默认或者相同priority的方式来进行hook。
扯了这么多，大家也别嫌麻烦，工欲善其事，必先利其器。现在再回到之前的代码。我们在beforeHookMethod里面调用了
```java
XposedBridge.log(lpparam.packageName + "调用getDeviceId()获取了imei");
```
XposedBridge也是rovo89开发的一个Xposed的辅助库，调用其log方法后可以在手机端的Xposed管理器里面显示相关信息，这一步的意思表示我们**监测了app调用android.telephony.TelephonyManager这个类的getDeviceId方法**

## 打印方法调用栈
上面的所有操作知识标记了调没调用指定的方法。但是如果调用了，是谁调用的，其实我们时不清楚的。这样其实不利于我们查找问题的根源。回看本文的第一张信通院的图，发现他们检测时，其实给了方法调用栈。那么我们现在就来模拟一下这种操作。
我们需要打印的是整个hook期间的方法栈，那么这个操作就应该放在afterHookMethod里面，于是，我们可以写成这样:
```java
XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getDeviceId",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getDeviceId()获取了imei");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        //在这里写调用方法栈过程
                    }
                }
        );
```
日志打印的话自然还是用到**XposedBridge**的**log**方法。由于我们需要hook的方法不止一个，而我们打印方法调用栈又是一样的操作，于是乎我们可以自己写一个抽象类继承**XC_MethodHook**,只实现**afterMethodHook**方法，在里面做统一的方法栈追踪操作。因此，我们先自定义一个**DumpMethodHook**的类，代码如下:
```java
public abstract class DumpMethodHook extends XC_MethodHook {

    /**
     * 该方法会在Hook了指定方法后调用
     * @param param
     */
    @Override
    protected void afterHookedMethod(MethodHookParam param) {
        //在这里，我们dump一下调用的方法栈信息
        dump2();
    }

    /**
     * dump模式一:根据线程进行过滤
     */
    private static void dump() {
        for (Map.Entry<Thread, StackTraceElement[]> stackTrace : Thread.getAllStackTraces().entrySet()) {
            Thread thread = (Thread) stackTrace.getKey();
            StackTraceElement[] stack = (StackTraceElement[]) stackTrace.getValue();
            // 进行过滤
            if (thread.equals(Thread.currentThread())) {
                continue;
            }
            XposedBridge.log("[Dump Stack]" + "**********线程名字：" + thread.getName() + "**********");
            int index = 0;
            for (StackTraceElement stackTraceElement : stack) {
                XposedBridge.log("[Dump Stack]" + index + ": " + stackTraceElement.getClassName()
                        + "----" + stackTraceElement.getFileName()
                        + "----" + stackTraceElement.getLineNumber()
                        + "----" + stackTraceElement.getMethodName());
            }
            // 增加序列号
            index++;
        }
        XposedBridge.log("[Dump Stack]" + "********************* over **********************");
    }

    /**
     * dump模式2：类信通院报告模式
     */
    private static void dump2(){
        XposedBridge.log("Dump Stack: "+"---------------start----------------");
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (int i= 0; i < stackElements.length; i++) {
                StringBuilder sb=new StringBuilder("[方法栈调用]");
                sb.append(i);
                XposedBridge.log("[Dump Stack]"+i+": "+ stackElements[i].getClassName()
                        +"----"+stackElements[i].getFileName()
                        +"----" + stackElements[i].getLineNumber()
                        +"----" +stackElements[i].getMethodName());
            }
        }
        XposedBridge.log("Dump Stack: "+ "---------------over----------------");
    }
}
```
通过查询资料，我写了两种方法栈打印的操作。第一种打印得比较细一些，但是实际测试要卡顿一点。第二种就和信通院报告差不多了，而且没有明显卡顿。
写好了自定义的回调，这时我们只需要将前面的XC_MethodHook替换为DumpMethodHook即可:
```java
XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getDeviceId",
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getDeviceId()获取了imei");
                    }
                }
        );
```

## 需要监测的方法
既然合规这件事情是工信部搞出来的，那么我们自然要看一下当时的这份红头文件——[工信部信管函「164号文」](https://www.waizi.org.cn/law/88720.html)
下面是我目前整理出来的需要hook的一些方法:
|方法名字|所属包名	|作用|
|--|--|--|
|getDeviceId|	android.telephony.TelephonyManager|	获取设备号|
|getDeviceId(int)|	android.telephony.TelephonyManager|	getDeviceId的带参版本|
|getImei|	android.telephony.TelephonyManager|	安卓8增加的获取IMEI的方法|
|getImei(int)|	android.telephony.TelephonyManager|	getImei的带参版本|
|getSubscriberId|	android.telephony.TelephonyManager|	获取IMSI|
|getMacAddress|	android.net.wifi.WifiInfo|	获取MAC地址|
|getHardwareAddress|	java.net.NetworkInterface|	获取MAC地址|
|getString|	android.provider.Settings.Secure|	获取系统相关信息字符来拼接deviceId|
|getLastKnownLocation|	LocationManager|	获取GPS定位信息|
|requestLocationUpdates|	LocationManager|	位置、时间发生改变的时候获取定位信息|
上面的方法信息可能不全，如果大家有更好的意见可以留言。我看网上很多资料是没有对**requestLocationUpdates**和安卓8的新增方法**getImei**进行监控的，这里我加了进来。

## 对Hook的APP进行过滤，设置白名单
一般来讲，你的手机安装的不止一个app。如果用上面的代码去监测，实际会监测你手机上所有的app。这就导致日志会很杂乱，我们其实只关心指定的app。因此我们需要设置一个白名单进行过滤:
```java
/**
  * 需要Hook的包名白名单
  */
 private static final String[] whiteList = {
         "com.cjs.drv",
         "com.cjs.hegui30.demo"
 };
```
里面填写的就是你需要监测的app的包名。
然后我们在HandleLoadPackage方法的最开始，写一段过滤的操作：
```java
/*判断hook的包名*/
boolean res = false;
for (String pkgName : whiteList) {
    if (pkgName.equals(lpparam.packageName)) {
        res = true;
        break;
    }
}
if (!res) {
    Log.e(TAG, "不符合的包:" + lpparam.packageName);
    return;
}
```
最终，贴上一个成品的代码:
```java
public class HookTrack implements IXposedHookLoadPackage {
    private static final String TAG = "HookTrack";

    /**
     * 需要Hook的包名白名单
     */
    private static final String[] whiteList = {
            "com.cjs.drv",
            "com.bw30.zsch",
            "com.bw30.zsch.magic",
            "com.cjs.hegui30.demo"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (lpparam == null) {
            return;
        }

        Log.e(TAG, "开始加载package:" + lpparam.packageName);
        /*判断hook的包名*/
        boolean res = false;
        for (String pkgName : whiteList) {
            if (pkgName.equals(lpparam.packageName)) {
                res = true;
                break;
            }
        }
        if (!res) {
            Log.e(TAG, "不符合的包:" + lpparam.packageName);
            return;
        }

        //固定格式
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(), // 需要hook的方法所在类的完整类名
                lpparam.classLoader,                            // 类加载器，固定这么写就行了
                "getDeviceId",                     // 需要hook的方法名
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getDeviceId()获取了imei");
                    }
                }
        );
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getDeviceId",
                int.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getDeviceId(int)获取了imei");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getSubscriberId",
                int.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getSubscriberId获取了imsi");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getImei",
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getImei获取了imei");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getImei",
                int.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getImei(int)获取了imei");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.net.wifi.WifiInfo.class.getName(),
                lpparam.classLoader,
                "getMacAddress",
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getMacAddress()获取了mac地址");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                java.net.NetworkInterface.class.getName(),
                lpparam.classLoader,
                "getHardwareAddress",
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getHardwareAddress()获取了mac地址");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.provider.Settings.Secure.class.getName(),
                lpparam.classLoader,
                "getString",
                ContentResolver.class,
                String.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用Settings.Secure.getstring获取了" + param.args[1]);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                LocationManager.class.getName(),
                lpparam.classLoader,
                "getLastKnownLocation",
                String.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getLastKnownLocation获取了GPS地址");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                LocationManager.class.getName(),
                lpparam.classLoader,
                "requestLocationUpdates",
                String.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用requestLocationUpdates获取了GPS地址");
                    }
                }
        );
    }
}
```

# 注册模块代码
上面的操作到目前为止也只是在你的安卓项目中添加了一个java类。如何让xposed识别到我们写的代码是个xposed模块呢？这就需要注册一下这个类。
注册分两步操作:
1、在AndroidManifest.xml中编写meta信息
```xml
<!--  标志该 apk 为一个 Xposed 模块，供 Xposed 框架识别-->
<meta-data
    android:name="xposedmodule"
    android:value="true" />

<!--模块说明，一般为模块的功能描述-->
<meta-data
    android:name="xposeddescription"
    android:value="这个模块是用来检测用户隐私合规的，在用户未授权同意前，调用接口获取信息属于违规" />

<!--模块兼容版本-->
<meta-data
    android:name="xposedminversion"
    android:value="54" />
```
在application节点里面加上这三个meta信息。那个说明会最终显示在xposed管理器上面:
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714160930818.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
**注意:填写meta信息是标记我们这个apk是个xposed模块的关键，否则xposed installer不会识别。**

2、在项目**asset**文件夹下面新建**xposed_init**文件
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714161140801.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
在里面写上我们实现IXposedHookLoadPackage那个类的包名+类名
```
com.cjs.hegui30.HookTrack
```
这样我们就写好了自定义的xposed模块。Xposed在加载的时候会从这个文件里面读取需要初始化的类。
至此，我们的所有代码就编写完成了，此时装在手机后，可以在xposed installer里面识别激活了。

# 其他
源码同时捆绑了一个快速测试的demo和相关的apk文件，demo可以单独编译成apk,记得切换
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021071416320853.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210714163221242.png#pic_center)

---

# 操作手册

# 一、准备条件
## 1、编译合规检测的Xposed模块源码

下载源码，修改设置白名单，编译成apk，安装到手机
相关操作参考[《安卓端自行实现工信部要求的隐私合规检测一(教你手写Xposed模块代码)》](https://blog.csdn.net/cjs1534717040/article/details/118721277)
> **注意:源码里面包含了各种安装包及demo**

## 2、已经root的手机可以下载Xposed.apk

Xposed在github上面开源，可以自己下载[**XposedInstaller**](https://github.com/rovo89/XposedInstaller)的源码进行编译，也可以直接下载已经编译好的apk。
> 需要注意的是，安卓5以上和以下的安装版本是不一样的
> [**支持安卓5及以上的XposedInstaller**](https://gitee.com/android_apes/Hegui3.0/raw/master/%E6%A3%80%E6%B5%8B%E5%BF%85%E8%A6%81%E6%96%87%E4%BB%B6/xposed.installer3.1.5%28%E9%80%82%E5%90%88%E5%AE%89%E5%8D%935%E4%BB%A5%E4%B8%8A%E7%9A%84%E6%89%8B%E6%9C%BA%29.apk) 
> [**支持安卓5以下的XposedInstaller**](https://gitee.com/android_apes/Hegui3.0/raw/master/%E6%A3%80%E6%B5%8B%E5%BF%85%E8%A6%81%E6%96%87%E4%BB%B6/xposed.installer_v33_36570c%28%E9%80%82%E5%90%88%E5%AE%89%E5%8D%935%E4%BB%A5%E4%B8%8B%E7%9A%84%E6%89%8B%E6%9C%BA%29.apk)

## 3、没有root的手机可以下载VirtualXposed.apk

VirtualXposed在github上面有专门的[**release**](https://github.com/android-hacker/VirtualXposed/releases)页面，其作者在0.20.x的版本的时候放弃了对32位应用的支持，理由是谷歌商店未来只允许64位的app上架，不想花更多精力维护32位的开发。如果你的app为32位应用，因此不能用0.20.x及之后版本的VirtualXposed
> **[0.20.3版本(支持64位应用)](https://gitee.com/android_apes/Hegui3.0/raw/master/%E6%A3%80%E6%B5%8B%E5%BF%85%E8%A6%81%E6%96%87%E4%BB%B6/VirtualXposed_0.20.3%28%E9%80%82%E5%90%8864%E4%BD%8Dapp%29.apk)**
> **[0.18.2版本(支持32位应用)](https://gitee.com/android_apes/Hegui3.0/raw/master/%E6%A3%80%E6%B5%8B%E5%BF%85%E8%A6%81%E6%96%87%E4%BB%B6/VirtualXposed_0.18.2%28%E9%80%82%E5%90%8832%E4%BD%8Dapp%29.apk)** 
> **注意:由于0.20.x版本更换了包名，所以它和0.18.x的版本能同时安装**

## 4、下载合规检测测试程序

该项非必须，仅作为合规快速校验 
> 下载地址:[**合规检测测试程序apk**](https://gitee.com/android_apes/Hegui3.0/raw/master/%E6%A3%80%E6%B5%8B%E5%BF%85%E8%A6%81%E6%96%87%E4%BB%B6/%E5%90%88%E8%A7%84%E6%A3%80%E6%B5%8B%E6%B5%8B%E8%AF%95%E7%A8%8B%E5%BA%8F.apk)
---
# 二、具体操作
> 这里以对“[**合规检测测试程序**](https://gitee.com/android_apes/Hegui3.0/raw/master/%E6%A3%80%E6%B5%8B%E5%BF%85%E8%A6%81%E6%96%87%E4%BB%B6/%E5%90%88%E8%A7%84%E6%A3%80%E6%B5%8B%E6%B5%8B%E8%AF%95%E7%A8%8B%E5%BA%8F.apk)“的校验进行说明。

## 1、安装
 - **已root的用户**

	

	 1. 安装xposed.apk

	 	

> 在root手机上安装xposed框架的操作可能会比较麻烦，详情问问度娘

	

	 2. 安装合规检测xposed模块.apk
	 3. 安装合规检测测试程序.apk

 - **未root用户**

	

	 1. 先安装virtual-xposed.apk
	 2. 后续操作有两种方案:

> 	第一种可以和已root用户一样，直接将合规检测xposed模块.apk和合规检测测试程序.apk安装在手机真机上，然后在virtual-xposed里面克隆这两个app;
> 第二种的话就是不在真机装，直接在virtual-xposed里面装，具体操作如下:

 1. 打开**virtual-xposed**，点击底部的**菜单**按钮，进入到virtual-xposed的菜单界面
<div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715095447417.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图1" align=center />
</div>

 2. 在菜单界面有两个关键点需要注意，一个是顶部的**添加应用**，一个是底部的**重启**。后者在激活模块的时候需要使用。这里我们先点击**添加应用**。
 <div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715100616238.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图2" align=center />
</div>

 3. 添加应用界面默认的tab项就是**克隆APP**，如果使用克隆的方式的话，就要先找到安装的合规检测xposed模块和合规检测测试程序，并勾选，然后选择底部的**安装**；如果选择的是虚拟机安装，就直接点击右下角的**加号**按钮，选择本地的apk安装包进行安装。
 <div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715101257676.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图3" align=center />
</div>

 4. 在克隆APP中，会弹出是否使用太极安装，我们还是选择**virtual-xposed安装**
 <div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715101819206.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图4" align=center />
</div>
 
 5. 安装完app后，下一步就是激活模块。我们返回到**1**中的界面，**上滑**，进入到app列表界面，在这里可以看见我们刚刚安装的3个app
 <div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715102701144.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图5" align=center />
</div>

 6. 最右边的那边Xposed Installer就是我们的xposed控制界面 ，点击进入。大大的绿勾表示我们的xposed框架已经激活。点击左上角**三杠**。
<div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715102939132.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图6" align=center />
</div>

 7. 在弹出来的菜单界面中有两个项要注意:**模块**用于**安装/卸载**xposed模块，**日志**用于查看合规检测的结果，后面会用到。这里我们点击**模块**。
<div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715103119338.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图7" align=center />
</div>
 
 8. 可以看见有一个合规检测的xposed模块，勾上它。勾上后需要重启系统。如果是root用户在真机下操作xposed框架，就需要真机重启。但是我们用的是virtual-xposed，所以只需要重启虚拟机就行了。这时候，返回到**2**的界面，点击**重启**。
<div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715103340219.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图8" align=center />
</div>
 
 9. 重启成功的话，底部会有提示语展示
<div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715103551253.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图9-1" align=center />
	<img src="https://img-blog.csdnimg.cn/20210715103629292.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图9-2" align=center />
</div>

## 2、如何检测

 1. 先打开我们安装的测试app

> 里面有四个按钮，分别对应四种不同的检测条件，我们以第一种**模拟获取IMSI**来说明，***先别点击任何按钮***。
<div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715103931453.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图10" align=center />
</div>

 2. 查看日志

回到我们在安装讲解中的第**7**步界面，里面有个**日志**选项，点击它。该界面右上角有**保存**按钮，还有个**三个点**的菜单按钮，点击菜单按钮后有两个项需要注意:

>  - 立即清理日志会清空当前界面的所有日志，我们先点击**清空一下**。
>  - **重新载入**会刷新最新的日志进界面。因为日志的显示不是自动的，要想看到最新的结果，就要手动刷新。
 <div align="center">
	<img src="https://img-blog.csdnimg.cn/20210715104617934.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图11-1" align=center />
	<img src="https://img-blog.csdnimg.cn/20210715104640552.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70" width = "300" alt="图11-2" align=center />
</div>

接下来回到我们的测试app界面，点击模拟获取IMSI，然后再返回到日志界面，并且重新载入，接着就能看见如下的日志记录:
![图12](https://img-blog.csdnimg.cn/20210715104836642.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NqczE1MzQ3MTcwNDA=,size_16,color_FFFFFF,t_70#pic_center)

上面的截图主要针对virtual-xposed来讲的。对于xposed而言，操作大同小异。最大的区别就是xposed需要真实地重启手机，这点的话要麻烦一点。