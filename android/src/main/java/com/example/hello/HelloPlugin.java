package com.example.hello;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** HelloPlugin */
public class HelloPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private Context context;
  private MethodChannel channel;

  private Intent batteryInfoIntent;

  private EventChannel.EventSink events;
  private BroadcastReceiver usbReceiver;


  // onAttachedToEngine 是 Flutter 插件中的一个生命周期方法，在 Flutter 插件开发中，用于插件和 Flutter 引擎（engine）之间的绑定。
  //  作用：
  //
  //  onAttachedToEngine 方法在插件与 Flutter 引擎成功连接时被调用。这通常发生在插件被加载到应用中时。它允许插件初始化与 Flutter 引擎的交互，如注册通道、设置消息传递等操作。
  //
  //  具体来说，onAttachedToEngine 方法通常用于以下操作：
  //
  //          •	注册 MethodChannel 或 EventChannel：设置与 Flutter 端的通信通道，监听来自 Flutter 的方法调用或事件。
  //          •	插件的初始化：进行与 Flutter 引擎的通信设置、插件资源的初始化等。
  //          •	Flutter 引擎的绑定：此方法会将插件绑定到 Flutter 引擎，使插件能够通过引擎与 Flutter 框架进行交互。
  //
  //  在插件中如何使用：
  //
  //  当 Flutter 插件附加到 Flutter 引擎时，onAttachedToEngine 方法被调用。这个方法接受一个 FlutterPluginBinding 对象，它提供了与 Flutter 引擎的交互接口。通过这个接口，插件可以注册与 Flutter 的通信通道，处理消息和事件流等。
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    // 在插件附加到 Flutter 引擎时执行初始化操作
    this.context = flutterPluginBinding.getApplicationContext();
    // 注册方法通道，处理来自 Flutter 的方法调用
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "hello.methodChannel");
    channel.setMethodCallHandler(this);
    // 注册事件通道，处理来自 Flutter 的事件流
    EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "hello.eventChannel");
    eventChannel.setStreamHandler((EventChannel.StreamHandler) this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("getPhoneBattery")) {
      result.success(getPhoneBattery(context));
    } else if (call.method.equals("getPhoneBatteryCharging")) {
      result.success(getPhoneBatteryCharging(context));
    } else {
      result.notImplemented();
    }
  }

  private int getPhoneBattery(Context context) {
    int level = 0;
//    BatteryManager batteryManager = (BatteryManager) context.getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
//    level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
//    return level
    Intent batteryInfoIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    int batteryPermission = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    if (batteryPermission != -1) {
      level = batteryInfoIntent.getIntExtra("level", 0);
      int batterySum = batteryInfoIntent.getIntExtra("scale", 100);
      return 100 * level / batterySum;
    } else {
      return 0;
    }
  }

  private String getPhoneBatteryCharging(Context context) {
    Intent batteryInfoIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    int status = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    String statusString;
    switch (status) {
      case BatteryManager.BATTERY_STATUS_CHARGING:
        statusString = "正在充电";
        break;
      case BatteryManager.BATTERY_STATUS_DISCHARGING:
        statusString = "正在放电";
        break;
      case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
        statusString = "没有充电";
        break;
      case BatteryManager.BATTERY_STATUS_FULL:
        statusString = "电量已满";
        break;
      case BatteryManager.BATTERY_STATUS_UNKNOWN:
      default:
        statusString = "电量未知";
        break;
    }
    Log.d("TAG", "电量状态变化为:" + status);
    return statusString;
  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    this.events = events;
    usbReceiver = createUsbReceiver();
    IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    context.getApplicationContext().registerReceiver(usbReceiver, filter);
  }

  private BroadcastReceiver createUsbReceiver() {
    return new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        Log.d("TAG", "充电状态变化为:" + chargePlug);
        String statusString;
        switch (chargePlug) {
          case BatteryManager.BATTERY_PLUGGED_AC:
            statusString = "正在电源充电";
            break;
          case BatteryManager.BATTERY_PLUGGED_USB:
            statusString = "设备正在通过 USB 端口充电";
            break;
          case BatteryManager.BATTERY_PLUGGED_WIRELESS:
            statusString = "设备正在通过无线充电";
            break;
          default:
            statusString = "设备充电未知";
            break;
        }
        Log.d("TAG", "充电状态变化为:" + chargePlug);
        events.success(statusString);
      }
    };
  }

  @Override
  public void onCancel(Object arguments) {
    context.getApplicationContext().unregisterReceiver(usbReceiver);
    usbReceiver = null;
  }
  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
