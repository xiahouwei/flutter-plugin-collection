import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:hello/hello.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _phoneBattery = 'Unknown';
  String _phoneBatterStatus = 'Unknown';
  String usbString = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
    Hello.eventStream.listen((event) {
      setState(() {
        usbString = event;
      });
    });
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    String phoneBattery;
    String phoneBatterStatus;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      int phoneBatteryInt = await Hello.getPhoneBattery;
      phoneBattery = '$phoneBatteryInt';
      platformVersion =
          await Hello.platformVersion ?? 'Unknown platform version';

      phoneBatterStatus = await Hello.getPhoneBatteryCharging ?? '无法获取';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
      phoneBattery = '获取失败';
      phoneBatterStatus = '获取失败';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
      _phoneBattery = phoneBattery;
      _phoneBatterStatus = phoneBatterStatus;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(children: [
            Text('Running on: $_platformVersion\n'),
            Text('电量: $_phoneBattery\n'),
            Text('充电状态: $_phoneBatterStatus\n'),
            Text('USB状态: $usbString\n'),
            ElevatedButton(
              child: Text('扫描'),
              onPressed: () {
                Hello.showScan();
              },
            )
          ]),
        ),
      ),
    );
  }
}
