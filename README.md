# PTQBookPageView

Android JetPack Compose 仿真书籍翻页组件，支持自定义任何非动态内容。

~~[效果演示] [DemoApk](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/app/release/PTQBookPageViewDemoApp.zip)~~

[项目仓库] [Github](https://github.com/FantasticPornTaiQiang/PTQFlipper)

[组件使用] [传送门](https://juejin.cn/post/7236281103221375033)

[设计思路] [传送门](https://juejin.cn/post/7236636296876818491)

[源码解析]

*   [算法部分](https://www.bilibili.com/video/BV1s24y1A7xM)
*   [绘制和动画部分](https://www.bilibili.com/video/BV1FP411X7Vu)（包括文字扭曲、点击动画、阴影等效果）

<img src="screenshot/demo.gif" height="500">

（DemoApk 中的图片均使用 AI 生成的图片）

---

## 1 使用组件

### 1.1 引入

1、在项目的 root 的 build.gradle 中引入仓库。

```groovy
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```

2、在项目的 app 的 build.gradle 中添加依赖。

```groovy
    dependencies {
        implementation 'com.github.FantasticPornTaiQiang:PTQFlipper:$latest_version'
    }
```

[最新版本](https://jitpack.io/#FantasticPornTaiQiang/PTQFlipper)

### 1.2 基本使用

在@Composable 中使用组件。

```kotlin
    val state by rememberPTQBookPageViewState(pageCount = 100)

    PTQBookPageView(state = state) {
        contents { currentPage, refresh ->
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                //任何非动态的自定义内容
            }

            refresh()
        }
    }
```

- 在 rememberPTQBookPageViewState 中**设置总页数**
- **请务必在 contents 的最末尾调用 refresh 方法以保证 View 显示的内容正常**
- 在 contents 中利用回调参数 currentPage 设置**当前页显示的内容**
- 在 contents 中，最外层是一个 fillMaxSize 的 Box，并需要使用 background 设置**当前纸页正面**的颜色
- 在 contents 的 Box 中（即示例代码的注释处），**可以自定义任何非动态的内容**

---

## 2 定制

### 2.1 页数控制

组件提供了[rememberPTQBookPageViewState](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/ptqbookpageview/src/main/java/ptq/mpga/ptqbookpageview/widget/PTQBookPageViewState.kt)方法进行页数相关的控制。

`rememberPTQBookPageViewState(pageCount: Int = 1, currentPage: Int? = null)`

- pageCount 总页数，如果页面总数小于当前页数，则会引发异常

- currentPage 当前页数，如果为 null 则页数由翻页器内部控制

控制页数方式有两种：

- 不传入 currentPage，则页数作为组件内部状态，由翻页器内部自动控制

- 传入 currentPage，则页数作为外部状态，需要在[onTurnPageRequest](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/ptqbookpageview/src/main/java/ptq/mpga/ptqbookpageview/widget/PTQBookPageViewScope.kt)方法中维护（[详细介绍](#24-翻页请求)）。

示例如下

```kotlin
    var state by rememberPTQBookPageViewState(pageCount = 100, currentPage = 0)

    PTQBookPageView(state = state) {
        onTurnPageRequest { currentPage, isNextOrPrevious, success ->
            if (!success) {
                Toast.makeText(ctx, if (isNextOrPrevious) "已经是最后一页啦" else "已经是第一页啦", Toast.LENGTH_SHORT).show()
            } else {
                state = state.copy(currentPage = if (isNextOrPrevious) currentPage + 1 else currentPage - 1)
            }
        }

        contents { currentPage, refresh ->
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                //任何非动态的自定义内容
                Text(currentPage.toString(), modifier = Modifier.align(Alignment.Center))
            }

            refresh()
        }
    }
```

### 2.2 配置参数

组件提供了[rememberPTQBookPageViewConfig](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/ptqbookpageview/src/main/java/ptq/mpga/ptqbookpageview/widget/PTQBookPageViewState.kt)方法进行相关的控制。

`rememberPTQBookPageViewConfig(pageColor: Color = Color.White, disabled: Boolean = false)`

- pageColor **当前页背面**的颜色（页面正面的颜色在 contents 的 Box 的 background modifier 中设置）（暂不支持页面透明度的设置，即alpha通道必须为ff）

- disabled 整个组件是否禁用（若想单独禁用单击或拖动手势，请看[高级定制](#3-高级定制)）

v1.1.0新增属性：

- distortionInterval 图像扭曲的采样间隔，可以根据屏幕大小去作适配，此值越小，曲线越精密，但同样地，计算开销会越大，性能会下降

- bezierEdgeDownSampling 边缘扭曲的采样间隔，可以根据屏幕大小去作适配，此值越大，曲线越精密，但同样地，计算开销会越大，性能会下降（变化不是线性的）

config 以参数的形式传入组件以生效，示例如下

```kotlin
    val config by rememberPTQBookPageViewConfig()

    PTQBookPageView(state = state, config = config) {
        ...
    }
```

### 2.3 大小和位置

组件支持以 modifier 的形式设置大小和位置，具体地

- 支持以 padding modifier 方式设置组件位置和大小

- 支持以 height modifier 和 width modifier 方式设置大小

- **不支持**offset 和 size modifier

示例如下

```kotlin
    PTQBookPageView(
        modifier = Modifier.padding(start = padding[0].dp, top = padding[1].dp, end = padding[2].dp, bottom = padding[3].dp),
        config = config,) {
        ...
    }
```

```kotlin
    PTQBookPageView(
        modifier = Modifier.width(100.dp).height(100.dp),
        config = config,) {
        ...
    }
```

### 2.4 翻页请求

在[PTQBookPageViewScope](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/ptqbookpageview/src/main/java/ptq/mpga/ptqbookpageview/widget/PTQBookPageViewScope.kt)中注册`onTurnPageRequest`回调以响应手指的翻页请求。

`onTurnPageRequest(block: (currentPage: Int, isNextOrPrevious: Boolean, success: Boolean) -> Unit)`

当用户有想翻页的操作时，会触发这个回调。如果当前处于最后一页，仍想向右翻，则会翻页失败，但此回调仍然会调用，可以利用这个回调弹 Toast 显示没有下一页了。

- currentPage 用户操作之后的页面索引，范围是 [0, pageCount)

- isNextOrPrevious 用户想向前翻页还是向后翻页，true 表示 next

- success 用户翻页是否成功，若处于最后一页还想向右翻则翻页失败，处于第一页向前翻同理

[示例代码](#21-页数控制)

---

## 3 高级定制

### 3.1 控制点击时的翻页行为

在[PTQBookPageViewScope](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/ptqbookpageview/src/main/java/ptq/mpga/ptqbookpageview/widget/PTQBookPageViewScope.kt)中注册`tapBehavior`回调以自定义控制点击时的行为。

`tapBehavior(block: (leftUp: Point, rightDown: Point, touchPoint: Point) -> Boolean?)`

- leftUP 组件左上角坐标

- rightDown 组件右下角坐标

- touchPoint 用户手指触摸坐标

- 返回值 翻页行为，true=下一页，false=上一页，null=不响应

此回调中的 Point 均为相对于组件左上角的坐标点，且此回调会影响 UI 的呈现和[onTurnPageRequest](#24-翻页请求)回调。

如果不注册此回调，则默认情况下：

- 点击 x>1/2 处时翻下一页

- 点击 x<1/2 处时翻上一页

[示例代码](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/app/src/main/java/ptq/mpga/pinance/NovelActivity.kt)

### 3.2 控制拖动时的起手响应

在[PTQBookPageViewScope](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/ptqbookpageview/src/main/java/ptq/mpga/ptqbookpageview/widget/PTQBookPageViewScope.kt)中注册`responseDragWhen`回调以自定义控制拖动时的起手响应。

`responseDragWhen(block: (rightDown: Point, startTouchPoint: Point, currentTouchPoint: Point) -> Boolean?)`

- rightDown 组件右下角坐标（左上角为 0）

- startTouchPoint 用户起手坐标（开始动画前）

- currentTouchPoint 当前触摸点坐标

- 返回值 翻页行为，true=下一页（从右侧翻开），false=上一页（从左侧翻开），null=不响应

此回调决定了从起手开始到能够自由拖动之间的翻页行为，如果不注册此回调，默认情况下：

- 从右往左滑则右起手（翻下一页）

- 从左往右划则左起手（翻上一页）

[示例代码](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/app/src/main/java/ptq/mpga/pinance/NovelActivity.kt)

### 3.3 控制拖动松手时的翻页行为

在[PTQBookPageViewScope](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/ptqbookpageview/src/main/java/ptq/mpga/ptqbookpageview/widget/PTQBookPageViewScope.kt)中注册`dragBehavior`回调以自定义控制拖动松手时的翻页行为。

```
dragBehavior(block: (
    rightDown: Point,
    initialTouchPoint: Point,
    lastTouchPoint: Point,
    isRightToLeftWhenStart: Boolean
    ) -> Pair<Boolean, Boolean?>
)
```

- rightDown 组件右下角坐标（左上角为 0）

- initialTouchPoint 用户初始触摸坐标（开始动画前）

- lastTouchPoint 用户松手坐标（结束动画前）

- isRightToLeftWhenStart 用户起手是从右向左还是从左向右（即 responseDragWhen 的结果）

- 返回值 翻页行为

  - 第一个参数控制 UI 动画，true=从左侧退出，false=从右侧退出

  - 第二个参数控制翻页响应，true=下一页，false=上一页，null=不翻页

此回调中的 Point 均为相对于组件左上角的坐标点，且此回调会影响 UI 的呈现和[onTurnPageRequest](#24-翻页请求)回调。

如果不注册此回调，则默认情况下：

- 起手从右向左

  - 松手在 x>1/2 处则不翻页

  - 松手在 x<1/2 处则翻下一页

- 起手从左向右

  - 松手在 x>1/2 处则翻上一页

  - 松手在 x<1/2 处则不翻页

[示例代码](https://github.com/FantasticPornTaiQiang/PTQFlipper/blob/main/app/src/main/java/ptq/mpga/pinance/NovelActivity.kt)

---

## 4 更新日志

**v1.0.0** 
- 初始版本，readme内的基本功能

**v1.0.1**
- 修复跟随算法bug

**v1.1.0**
- 配置项新增distortionInterval和bezierEdgeDownSampling
- 性能优化
- 移除了minSdk26的限制