package com.airbiquity.hap.tts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Button mPcmRecorderBtn;
	private Button mPcmPlayerBtn;
	private Button mSpxTts;
	private Button mPcmTts;

	private PcmRecorder mPcmRecorder;
	private PcmPlayer mPcmPlayer;

//	private SpeexRecorder spxRecorder;
//	private SpeexPlayer spxPlayer;

	// http://nissanmipdevgw.airbiquity.com:9018/mip_services/core/api/1.0/speech/text_to_speech?device_id=1234567890124212&language=en_US&accept_format=audio%2Fx-speex%3Brate%3D16000
	private final static String BASE_URL = "http://nissanmipdevgw.airbiquity.com:9018/";
	private final static String TTS_SERVICE = "mip_services/core/api/1.0/speech/text_to_speech";
	private final static String Language = "en_US";
	private final static String Accept_Format = "audio%2Fx-speex%3Brate%3D16000";
	private final static String MIP_ID = "1234";
	private final static String Content_Type = "text/plain";
	private final static String Input_Text = "how are you doing?This needs more information. ";
	private String device_id;
	
	private final static String[] Test_Text = new String[]{
		"A daily dose of vitamin D could actually increase a man's risk of prostate cancer a study out today shows researchers discovered the disturbing link while studying the effects of antioxidants on men's health",
		"A Texas woman is suing continental airlines and three other carriers over mental trauma she said she experienced during turbulence on a flight",
		"Although if we've breakfast or Wi-Fi might not be a dealbreaker for hotel stay try to choose hotels with the services to avoid piling up extra fees",
		"Family vacations are a great way to build memories with your kids are planning them can be complicated and tiring",
		"Users can use their voices to send e-mails and text messages schedule meetings place phone calls and more productivity tasks",
		"Out for a night on the town",
		"You're running and you need to fire off an e-mail to the staff",
		"No one wants to spend more than they have to on a vacation",
		"Offense start Detroit won't beat Texas in game three",
		"Washington man sets world record as oldest person to summit Mount Kilimanjaro",
		"Destination home",
		"Destination work",
		"Facebook posting",
		"Facebook wall",
		"Local search bookstore"
	};
	
	private String basePath;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		device_id = TelephonyMgr.getDeviceId();
		
		basePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/ttsdemo";
		File baseFolder = new File(basePath);
		if(!baseFolder.exists()){
			baseFolder.mkdir();
		}

		mPcmRecorderBtn = (Button) findViewById(R.id.pcm_recorder);
		mPcmPlayerBtn = (Button) findViewById(R.id.pcm_player);
		
		mSpxTts = (Button) findViewById(R.id.spx_tts);
		mPcmTts = (Button) findViewById(R.id.pcm_tts);

		mPcmRecorderBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				mPcmRecorder = new PcmRecorder();
				new Thread(mPcmRecorder).start();

				mPcmRecorder.setRecording(true);
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				ProgressBar pb = new ProgressBar(MainActivity.this);
				builder.setTitle("Please Speaking");
				builder.setView(pb);
				builder.setPositiveButton("Finish",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (mPcmRecorder.isRecording()) {
									mPcmRecorder.setRecording(false);
								}
							}
						});
				builder.show();
			}
		});

		mPcmPlayerBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				mPcmPlayer = new PcmPlayer();
				new Thread(mPcmPlayer).start();
				mPcmPlayer.setPlaying(true);

				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				ProgressBar pb = new ProgressBar(MainActivity.this);
				builder.setTitle("Playing");
				builder.setView(pb);
				builder.setPositiveButton("close",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (mPcmPlayer.isPlaying()) {
									mPcmPlayer.setPlaying(false);
								}
							}
						});
				builder.show();
			}
		});

		mSpxTts.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				new Thread(){

					@Override
					public void run() {
						
						basePath +="/spx";
						File pcmFolder = new File(basePath);
						if(!pcmFolder.exists()){
							pcmFolder.mkdir();
						}
						
						
						FileOutputStream resultWriter = null;
						try {
							File result = new File(basePath+"/spx_result.csv");
							if(!result.exists()){
								result.createNewFile();
							}
							resultWriter = new FileOutputStream(result);
							String header = "Text"+","+"requestStart"+","+"responseStart"+","+"responseEnd"+","+"Audio(Spx)"+"\n";
							resultWriter.write(header.getBytes());
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						
						for(int i = 0; i<Test_Text.length;i++){
							// Log.d("TAG", device_id);
							// String accpet_format = URLEncoder.encode("audio/x-wav;codec=pcm;bit=16;rate=16000");
							String accpet_format = URLEncoder.encode("audio/x-speex;rate=16000");
							Log.d("TAG", accpet_format);
							// Log.d("TAG", Accept_Format);

							HttpClient client = new DefaultHttpClient();
							try {
								StringBuffer url = new StringBuffer();
								url.append(BASE_URL).append(TTS_SERVICE)
										.append("?").append("device_id=")
										.append(device_id).append("&language=")
										.append(Language)
										.append("&accept_format=")
										.append(accpet_format);

								Log.d("TAG", url.toString());
								
								HttpPost request = new HttpPost(url.toString());
								request.setHeader("mip-id", MIP_ID);
								request.setHeader("Content-Type", Content_Type);

								request.setEntity(new StringEntity(Test_Text[i],"UTF-8"));
								long requestStart = System.currentTimeMillis();
								HttpResponse response = client.execute(request);
								long responseStart = System.currentTimeMillis();
								long responseEnd = 0;
								int statusCode = response.getStatusLine().getStatusCode();
								Log.d("TAG", "status = " + statusCode);
								HttpEntity entity = response.getEntity();
								if (entity != null) {

									InputStream content = entity.getContent();

									// OggPlayer oggPlayer = new
									// OggPlayer(content);
									// new Thread(oggPlayer).start();
									// oggPlayer.setPlaying(true);
									
									File file = new File(basePath+"/Response"+i+".spx");
									if(!file.exists()){
										file.createNewFile();
									}
									FileOutputStream fos = new FileOutputStream(file);
									
									int count = 0;
									byte[] buf = new byte[8092];
									while ((count = content.read(buf)) != -1) {
										fos.write(buf, 0, count);
									}
									fos.flush();
									fos.close();
									responseEnd = System.currentTimeMillis();
								}
								String str = Test_Text[i]+","+requestStart+","+responseStart+","+responseEnd+","+"Response"+i+".spx"+"\n";
								
								resultWriter.write(str.getBytes());

							} catch (ClientProtocolException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								client.getConnectionManager().closeExpiredConnections();
							}

						}
						
						Log.d("TAG", "Test Spx Finished!");
					}
					
				}.start();
				

			}
		});

		mPcmTts.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
				
				new Thread(){

					@Override
					public void run() {
						
						basePath +="/pcm";
						File pcmFolder = new File(basePath);
						if(!pcmFolder.exists()){
							pcmFolder.mkdir();
						}
						
						FileOutputStream resultWriter = null;
						try {
							File result = new File(basePath+"/pcm_result.csv");
							if(!result.exists()){
								result.createNewFile();
							}
							resultWriter = new FileOutputStream(result);
							String header = "Text"+","+"requestStart"+","+"responseStart"+","+"responseEnd"+","+"Audio(Pcm)"+"\n";
							resultWriter.write(header.getBytes());
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						
						for(int i = 0; i<Test_Text.length;i++){
							// Log.d("TAG", device_id);
							String accpet_format = URLEncoder.encode("audio/x-wav;codec=pcm;bit=16;rate=16000");
							// String accpet_format = URLEncoder.encode("audio/x-speex;rate=16000");
							Log.d("TAG", accpet_format);
							// Log.d("TAG", Accept_Format);

							HttpClient client = new DefaultHttpClient();
							try {
								StringBuffer url = new StringBuffer();
								url.append(BASE_URL).append(TTS_SERVICE)
										.append("?").append("device_id=")
										.append(device_id).append("&language=")
										.append(Language)
										.append("&accept_format=")
										.append(accpet_format);

								Log.d("TAG", url.toString());
								
								HttpPost request = new HttpPost(url.toString());
								request.setHeader("mip-id", MIP_ID);
								request.setHeader("Content-Type", Content_Type);

								request.setEntity(new StringEntity(Test_Text[i],"UTF-8"));
								long requestStart = System.currentTimeMillis();
								HttpResponse response = client.execute(request);
								long responseStart = System.currentTimeMillis();
								long responseEnd = 0;
								int statusCode = response.getStatusLine().getStatusCode();
								Log.d("TAG", "status = " + statusCode);
								HttpEntity entity = response.getEntity();
								if (entity != null) {

									InputStream content = entity.getContent();

									// OggPlayer oggPlayer = new
									// OggPlayer(content);
									// new Thread(oggPlayer).start();
									// oggPlayer.setPlaying(true);
									
									File file = new File(basePath+"/Response"+i+".pcm");
									if(!file.exists()){
										file.createNewFile();
									}
									FileOutputStream fos = new FileOutputStream(file);
									
									int count = 0;
									byte[] buf = new byte[8092];
									while ((count = content.read(buf)) != -1) {
										fos.write(buf, 0, count);
									}
									fos.flush();
									fos.close();
									responseEnd = System.currentTimeMillis();
								}
								
								String str = Test_Text[i]+","+requestStart+","+responseStart+","+responseEnd+","+"Response"+i+".pcm"+"\n";
								resultWriter.write(str.getBytes());

							} catch (ClientProtocolException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								client.getConnectionManager().closeExpiredConnections();
							}

						}
						
						try {
							resultWriter.flush();
							resultWriter.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						//Toast.makeText(MainActivity.this, "Test Finished", Toast.LENGTH_LONG).show();
						Log.d("TAG", "Test Pcm Finished!");
					}
					
				}.start();


			}
		});


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
