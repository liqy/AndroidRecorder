package com.airbiquity.hap.tts;

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
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

	private Button mPcmRecorderBtn;
	private Button mPcmPlayerBtn;
	private Button mPcmTts;

	private Button mSpxEncoder;
	private Button mSpxDecoder;

	private PcmRecorder mPcmRecorder;
	private PcmPlayer mPcmPlayer;

	private SpeexRecorder spxRecorder;
	private SpeexPlayer spxPlayer;

	// http://nissanmipdevgw.airbiquity.com:9018/mip_services/core/api/1.0/speech/text_to_speech?device_id=1234567890124212&language=en_US&accept_format=audio%2Fx-speex%3Brate%3D16000
	private final static String BASE_URL = "http://nissanmipdevgw.airbiquity.com:9018/";
	private final static String TTS_SERVICE = "mip_services/core/api/1.0/speech/text_to_speech";
	private final static String Language = "en_US";
	private final static String Accept_Format = "audio%2Fx-speex%3Brate%3D16000";
	private final static String MIP_ID = "1234";
	private final static String Content_Type = "text/plain";
	private final static String Input_Text = "how are you doing?This needs more information. ";

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mPcmRecorderBtn = (Button) findViewById(R.id.pcm_recorder);
		mPcmPlayerBtn = (Button) findViewById(R.id.pcm_player);
		mPcmTts = (Button) findViewById(R.id.pcm_tts);

		mSpxEncoder = (Button) findViewById(R.id.speexEncoder);
		mSpxDecoder = (Button) findViewById(R.id.speexDecoder);

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

		mPcmTts.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
				String device_id = TelephonyMgr.getDeviceId();
				Log.d("TAG", device_id);

//				String accpet_format = URLEncoder
//						.encode("audio/x-wav;codec=pcm;bit=16;rate=16000");
				String accpet_format = URLEncoder.encode("audio/x-speex;rate=16000");
				Log.d("TAG", accpet_format);
				Log.d("TAG", Accept_Format);

				HttpClient client = new DefaultHttpClient();
				try {
					StringBuffer url = new StringBuffer();
					url.append(BASE_URL).append(TTS_SERVICE).append("?")
							.append("device_id=").append(device_id)
							.append("&language=").append(Language)
							.append("&accept_format=").append(accpet_format);

					Log.d("TAG", url.toString());
					HttpPost request = new HttpPost(url.toString());
					request.setHeader("mip-id", MIP_ID);
					request.setHeader("Content-Type", Content_Type);

					request.setEntity(new StringEntity(Input_Text, "UTF-8"));
					HttpResponse response = client.execute(request);

					int statusCode = response.getStatusLine().getStatusCode();

					Log.d("TAG", "status = " + statusCode);

					HttpEntity entity = response.getEntity();
					if (entity != null) {

						InputStream content = entity.getContent();
						
						OggPlayer oggPlayer = new OggPlayer(content);
						new Thread(oggPlayer).start();
						oggPlayer.setPlaying(true);

						// SpxPlayer spxPlayer = new SpxPlayer(content);
						// new Thread(spxPlayer).start();
						// spxPlayer.setPlaying(true);

						// PcmWriter pcmPlayer = new PcmWriter();
						// new Thread(pcmPlayer).start();
						// pcmPlayer.setPlaying(true);

						// try {
						//
						// int all = 0;
						// byte[] b = new byte[8092];
						// int count = 0;
						// while((count = content.read(b))!=-1){
						// Log.d("TAG", new String(b,0,count));
						// all += count;
						// Log.d("TAG", "count = "+count);
						// Log.d("TAG", "all = "+all);
						// pcmPlayer.putData(b, count);
						// }
						// pcmPlayer.setPlaying(false);
						// Log.d("--->", "total count = "+all+"");
						// } catch (IllegalArgumentException e) {
						// e.printStackTrace();
						// } catch (IllegalStateException e) {
						// e.printStackTrace();
						// }

					}

				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					client.getConnectionManager().closeExpiredConnections();
				}

			}
		});

		mSpxEncoder.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				spxRecorder = new SpeexRecorder();
				new Thread(spxRecorder).start();
				spxRecorder.setRecording(true);

				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				ProgressBar pb = new ProgressBar(MainActivity.this);
				builder.setTitle("Please Speaking");
				builder.setView(pb);
				builder.setPositiveButton("Finish",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (spxRecorder.isRecording()) {
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

				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);
				ProgressBar pb = new ProgressBar(MainActivity.this);
				builder.setTitle("Playing");

				builder.setView(pb);
				builder.setPositiveButton("close",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (spxPlayer.isPlaying()) {
									spxPlayer.setPlaying(false);
								}
							}
						});
				builder.show();
			}

		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
