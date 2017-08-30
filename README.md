1、封包zxing做的一个扫码库项目。

2、导入库再再加如下几行代码就可以实现扫码功能。

YiMaDecoder yima = YiMaDecoder.getInstance(MainActivity.this);

yima.addResultListener(MainActivity.this);

yima.scanBarcode()

manifest文件中加 

<activity android:name="com.yima.camera.CaptureActivity" />
