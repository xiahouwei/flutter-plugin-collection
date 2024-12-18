package com.example.hello;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.shouzhong.scanner.Callback;
import com.shouzhong.scanner.IViewFinder;
import com.shouzhong.scanner.ScannerView;

import cn.iwgang.licenseplatediscern.view.LicensePlateDiscernView;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.plugin.common.PluginRegistry;

/** HelloPlugin */
public class HelloPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native
  /// Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine
  /// and unregister it
  /// when the Flutter Engine is detached from the Activity
  private Context context;
  private MethodChannel channel;

  private Intent batteryInfoIntent;

  private EventChannel.EventSink events;
  private BroadcastReceiver usbReceiver;

  private Activity activity;

  private LicensePlateDiscernView cvLicensePlateDiscernView;

  private ScannerView scannerView;
  private Vibrator vibrator;

  private static final int SCAN_REQUEST_CODE = 1001;
  private static Result pendingResult;

  public static Result getPendingResult() {
    return pendingResult;
  }

  // onAttachedToEngine 是 Flutter 插件中的一个生命周期方法，在 Flutter 插件开发中，用于插件和 Flutter
  // 引擎（engine）之间的绑定。
  // 作用：
  //
  // onAttachedToEngine 方法在插件与 Flutter 引擎成功连接时被调用。这通常发生在插件被加载到应用中时。它允许插件初始化与
  // Flutter 引擎的交互，如注册通道、设置消息传递等操作。
  //
  // 具体来说，onAttachedToEngine 方法通常用于以下操作：
  //
  // • 注册 MethodChannel 或 EventChannel：设置与 Flutter 端的通信通道，监听来自 Flutter 的方法调用或事件。
  // • 插件的初始化：进行与 Flutter 引擎的通信设置、插件资源的初始化等。
  // • Flutter 引擎的绑定：此方法会将插件绑定到 Flutter 引擎，使插件能够通过引擎与 Flutter 框架进行交互。
  //
  // 在插件中如何使用：
  //
  // 当 Flutter 插件附加到 Flutter 引擎时，onAttachedToEngine 方法被调用。这个方法接受一个
  // FlutterPluginBinding 对象，它提供了与 Flutter 引擎的交互接口。通过这个接口，插件可以注册与 Flutter
  // 的通信通道，处理消息和事件流等。
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
    // LicensePlateDiscernCore.Companion.init(this.context);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("getPhoneBattery")) {
      result.success(getPhoneBattery(context));
    } else if (call.method.equals("getPhoneBatteryCharging")) {
      result.success(getPhoneBatteryCharging(context));
    } else if (call.method.equals("showScan")) {
      showScan(result);
    } else {
      result.notImplemented();
    }
  }

  // private void showScan () {
  // activity.runOnUiThread(() -> {
  // activity.setContentView(R.layout.activity_main);
  // // 通过 ID 获取 LicensePlateDiscernView
  // cvLicensePlateDiscernView =
  // activity.findViewById(R.id.cv_licensePlateDiscernView);
  // // 设置车牌识别回调监听器
  // cvLicensePlateDiscernView.setOnDiscernListener(new
  // Function1<LicensePlateInfo, Unit>() {
  // @Override
  // public Unit invoke(LicensePlateInfo result) {
  // // 处理回调逻辑
  // String scanResult = "识别结果：" + result.getLicensePlate() + "（可信度：" +
  // result.getConfidence() + "）";
  // System.out.println(scanResult);
  //
  // // 重新识别
  // cvLicensePlateDiscernView.reDiscern();
  //
  // return Unit.INSTANCE; // 必须返回 Unit.INSTANCE
  // }
  // });
  // });
  // }
  // private void showScan (@NonNull Result result) {
  // activity.runOnUiThread(() -> {
  // if (scannerView == null) {
  // activity.setContentView(R.layout.activity_main);
  // scannerView = activity.findViewById(R.id.sv);
  // scannerView.setShouldAdjustFocusArea(true);
  // scannerView.setViewFinder(new ViewFinder2());
  // scannerView.setRotateDegree90Recognition(true);
  // scannerView.setEnableLicensePlate(true);
  // scannerView.setCallback(new Callback() {
  // @Override
  // public void result(com.shouzhong.scanner.Result scanResult) {
  // startVibrator();
  // result.success(scanResult.data);
  // closeScan();
  // }
  // });
  // scannerView.onResume();
  // } else {
  // scannerView.setVisibility(View.VISIBLE);
  // }
  // });
  // }
  private void showScan(@NonNull Result result) {
    activity.runOnUiThread(() -> {
      // activity.setContentView(R.layout.activity_main);
      // Button backButton = activity.findViewById(R.id.backButton);
      // backButton.setOnClickListener(new View.OnClickListener() {
      // @Override
      // public void onClick(View v) {
      // }
      // });
      pendingResult = result;
      Intent intent = new Intent(context, NativeActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
    });
  }

  // @Override
  // public boolean onActivityResult(int requestCode, int resultCode, Intent data)
  // {
  // System.out.println("=====================================================");
  // if (requestCode == SCAN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
  // if (data != null) {
  // String scanResult = data.getStringExtra("scanResult");
  // pendingResult.success(scanResult); // 通过 MethodChannel.Result 回传给 Flutter
  // } else {
  // pendingResult.success("没有数据");
  // }
  // pendingResult = null; // 防止多次调用
  // }
  // return false;
  // }

  private void closeScan() {
    activity.runOnUiThread(() -> {
      // // 结束当前的 Activity
      // if (scannerView != null) {
      // scannerView.setVisibility(View.GONE);
      // }
    });
  }

  private void startVibrator() {
    if (vibrator == null)
      vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(300);
  }

  class ViewFinder2 implements IViewFinder {
    @Override
    public Rect getFramingRect() {
      return new Rect(240, 240, 840, 840);
    }
  }

  private int getPhoneBattery(Context context) {
    int level = 0;
    // BatteryManager batteryManager = (BatteryManager)
    // context.getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
    // level =
    // batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    // return level
    Intent batteryInfoIntent = context.getApplicationContext().registerReceiver(null,
        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
    Intent batteryInfoIntent = context.getApplicationContext().registerReceiver(null,
        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {
    this.activity = null;
  }
}
