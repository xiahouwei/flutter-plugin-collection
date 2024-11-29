package com.example.hello;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.shouzhong.scanner.IViewFinder;
import com.shouzhong.scanner.ScannerView;
import com.shouzhong.scanner.Callback;

import io.flutter.plugin.common.MethodChannel.Result;

public class NativeActivity extends AppCompatActivity {

  private ScannerView scannerView;
  private Vibrator vibrator;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);  // 设置布局
    showScan();
    Button backButton = findViewById(R.id.backButton);  // 假设你有一个返回按钮

    backButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Result result = HelloPlugin.getPendingResult();
        if (result != null) {
          // 返回成功结果到 Flutter
          result.success("取消识别拍照");
        }
        closeScan();  // 关闭当前原生页面并返回到 Flutter 页面
      }
    });
  }

  private void showScan () {
    scannerView = findViewById(R.id.sv);
    scannerView.setShouldAdjustFocusArea(true);
    scannerView.setViewFinder(new ViewFinder2());
    scannerView.setRotateDegree90Recognition(true);
    scannerView.setEnableLicensePlate(true);
    scannerView.setCallback(new Callback() {
      @Override
      public void result(com.shouzhong.scanner.Result scanResult) {
        startVibrator();
        Result result = HelloPlugin.getPendingResult();
        if (result != null) {
          // 返回成功结果到 Flutter
          result.success(scanResult.data);
        }
        closeScan();
      }
    });
    scannerView.onResume();
  }

  private void closeScan() {
    finish();
  }

  private void startVibrator() {
    if (vibrator == null)
      vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(300);
  }


  class ViewFinder2 implements IViewFinder {
    @Override
    public Rect getFramingRect() {
      return new Rect(240, 240, 840, 840);
    }
  }
}