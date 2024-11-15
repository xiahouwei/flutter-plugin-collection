import 'dart:async';

import 'package:flutter/services.dart';

class Hello {
  // 注册方法
  static const MethodChannel _channel = MethodChannel('hello.methodChannel');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<int> get getPhoneBattery async {
    final int battery = await _channel.invokeMethod('getPhoneBattery');
    return battery;
  }

  static Future<String?> get getPhoneBatteryCharging async {
    final String? result =
        await _channel.invokeMethod('getPhoneBatteryCharging');
    return result;
  }

  // 注册事件
  static const EventChannel _eventChannel = EventChannel('hello.eventChannel');

  // 监听事件流
  static Stream<String> get eventStream {
    return _eventChannel
        .receiveBroadcastStream()
        .map((event) => event as String);
    // return _eventChannel.receiveBroadcastStream();
  }
}
