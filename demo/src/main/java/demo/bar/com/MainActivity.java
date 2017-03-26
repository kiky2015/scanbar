package demo.bar.com;

import java.io.InputStream;

import com.yima.camera.YiMaDecoder;
import com.yima.listener.PluginResultListener;
import com.yima.listener.YiMaInfo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements PluginResultListener{

	private ImageView imageViewDec = null;
	private YiMaDecoder yima = null;
	private Button buttonDecode = null;
	private TextView textViewSymb = null;
	private TextView textViewRes = null;
	private TextView textViewDecTime = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initGUI();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		yima = YiMaDecoder.getInstance(MainActivity.this);
		yima.addResultListener(MainActivity.this);
	}

	private void initGUI() {
		imageViewDec = (ImageView) findViewById(R.id.imageViewDec);
		textViewSymb = (TextView) findViewById(R.id.textViewSymb);
		textViewRes = (TextView) findViewById(R.id.textViewRes);
		buttonDecode = (Button) findViewById(R.id.buttonDecode);
		textViewDecTime = (TextView) findViewById(R.id.textViewDecTime);
		buttonDecode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				yima.scanBarcode();
			}
		});
	}

	@Override
	public void onPluginResult(YiMaInfo arg0) {
		if(imageViewDec != null) {
			imageViewDec.setImageBitmap(arg0.barcodeImage);
			textViewSymb.setText("类型: "+arg0.barcodeType);
			textViewRes.setText("值: "+arg0.barcodeValue);
			textViewDecTime.setText("解码时间:"+arg0.barcodeDecodeTime+"ms");
		}
	}

}
