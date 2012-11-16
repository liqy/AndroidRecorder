package com.airbiquity.hap.tts;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xiph.speex.SpeexDecoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;

public class MainActivity extends Activity {
	
	private Button mStart;
	private Button mPlay;
	private Button mTts;
	private Button mSpxEncoder;
	private Button mSpxDecoder;
	
	private Recorder recorder ;
	private Player player ;
	
	private SpeexRecorder spxRecorder;
	private SpeexPlayer spxPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mStart = (Button) findViewById(R.id.start);
        mPlay = (Button) findViewById(R.id.play);
        mTts = (Button) findViewById(R.id.tts);
        mSpxEncoder = (Button) findViewById(R.id.speexEncoder);
        mSpxDecoder = (Button) findViewById(R.id.speexDecoder);
        
		mSpxEncoder.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
				spxRecorder = new SpeexRecorder();
				new Thread(spxRecorder).start();
				spxRecorder.setRecording(true);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				ProgressBar pb = new ProgressBar(MainActivity.this);
				builder.setTitle("Please Speaking");
				builder.setView(pb);
				builder.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if(spxRecorder.isRecording()){
							spxRecorder.setRecording(false);
						}
					}
				});
				builder.show();

			}
		});
		
		mSpxDecoder.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
					spxPlayer = new SpeexPlayer();
					new Thread(spxPlayer).start();
					spxPlayer.setPlaying(true);
					
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					ProgressBar pb = new ProgressBar(MainActivity.this);
					builder.setTitle("Playing");
					
					builder.setView(pb);
					builder.setPositiveButton("close", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if(spxPlayer.isPlaying()){
								spxPlayer.setPlaying(false);
							}
						}
					});
					builder.show();
				}

		});
        
		mStart.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				recorder = new Recorder();
				new Thread(recorder).start();
				recorder.setRecording(true);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				ProgressBar pb = new ProgressBar(MainActivity.this);
				builder.setTitle("Please Speaking");
				builder.setView(pb);
				builder.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if(recorder.isRecording()){
							recorder.setRecording(false);
						}
					}
				});
				builder.show();
			}
		});
		
		mPlay.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				player = new Player();
				new Thread(player).start();
				player.setPlaying(true);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				ProgressBar pb = new ProgressBar(MainActivity.this);
				builder.setTitle("Playing");
				
				builder.setView(pb);
				builder.setPositiveButton("close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if(player.isPlaying()){
							player.setPlaying(false);
						}
					}
				});
				builder.show();
			}
		});
        
		
		mTts.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
				HttpClient client = new DefaultHttpClient();
				try {
					  
					
					StringBuffer uri = new StringBuffer();
				 	uri.append(TTS_SERVICE)
					   .append(MIP_ID)
					   .append("?")
					   .append("deviceId=")
					   .append(Device_ID)
					   .append("&ttsLang=")
					   .append(TTS_Lang)
					   .append("&acceptFormat=")
					   .append(Accept_Format)
					   .append("&inputText=")
					   .append(Input_Text);
				 	
				 	Log.d("TAG", uri.toString());
				 	String encodedUri = URLEncoder.encode(uri.toString());
				 	Log.d("TAG", encodedUri);  
					  
				 	StringBuffer  sb = new StringBuffer();
				 	sb.append(BASE_URL).append(encodedUri);
					Log.d("TAG", sb.toString());  
					  
					
					
					HttpPost request = new HttpPost(sb.toString());
					request.setHeader("mip-id", MIP_ID);
					HttpResponse response = client.execute(request);
					int statusCode = response.getStatusLine().getStatusCode();
					
					Log.d("TAG", "status = "+statusCode);
					String reasonPhrase = response.getStatusLine().getReasonPhrase();
					Log.d("TAG", "reasonPhrase = "+reasonPhrase);
					long contentLength = response.getEntity().getContentLength();
					Log.d("TAG", "contentLength = "+contentLength);
//					InputStream is= response.getEntity().getContent();
//					byte[] b = new byte[1024];
//					int count = 0;
//					while((count = is.read(b))!=-1){
//						Log.d("TAG", new String(b,0,count));
//					}
					
					SpeexDecoder  speexDecoder = new SpeexDecoder();
					
					
					
					
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}finally{
					client.getConnectionManager().closeExpiredConnections();
				}
				

			}
		});
		
	}
    
    private final static String BASE_URL = "http://nissanmipdevtas.airbiquity.com:9016/";
    private final static String ACCOUNT_SERVICE = "account_services/api/1.0/";
    
    private final static String MIP_ID = "17bc2e45-2f4a-11e2-bea7-356cf6254872";
    
    private final static String ACCOUNT_MGR = "acocount";
    private final static String LOGIN = "login";
    
    private final static String TTS_SERVICE = "mip_services/core/api/1.0/voice/tts/mipId/";
    private final static String Device_ID = "1234567890124212";
    private final static String TTS_Lang = "en_US";
    private final static String Accept_Format = "audio/x-speex;rate=8000";
    private final static String Input_Text = "how are you doing";
    
    
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    

	
}
