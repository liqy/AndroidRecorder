package com.airbiquity.hap.tts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
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

public class MainActivity extends Activity{

	private Button mPcmRecorderBtn;
	private Button mPcmPlayerBtn;
	private Button mSpxTts;
	private Button mPcmTts;
	private Button mSpeaker;
	private Button mPcmVr;
	private Button mFilesVr;

	private PcmRecorder mPcmRecorder;
	private PcmPlayer mPcmPlayer;
	private PcmRecorderWithVad mPcmRecorderWithVad; 

//	private SpeexRecorder spxRecorder;
//	private SpeexPlayer spxPlayer;

	// http://nissanmipdevgw.airbiquity.com:9018/mip_services/core/api/1.0/speech/text_to_speech?device_id=1234567890124212&language=en_US&accept_format=audio%2Fx-speex%3Brate%3D16000
	private final static String BASE_URL = "http://nissanmipdevgw.airbiquity.com:9018/";
	private final static String TTS_SERVICE = "mip_services/core/api/1.0/speech/text_to_speech";
	private final static String Language = "en_US";
	private final static String Accept_Format = "audio%2Fx-speex%3Brate%3D16000";
	//private final static String MIP_ID = "4f70d255-3fc4-11e2-b89c-57a47db0387d";
	private final static String MIP_ID = "1234";
	private final static String Content_Type = "text/plain";
	private final static String HU_ID = "1234567890";
	private String device_id;
	
	private String basePath;
	
	private final static int Times = 10;
	private final static String Input_Text = "how are you doing?This needs more information. ";
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
	
	
	private final static String STT_SERVICE = "mip_services/core/api/1.0/speech/speech_to_text";  

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
		mSpeaker = (Button) findViewById(R.id.speaker);
		mPcmVr = (Button)findViewById(R.id.pcm_vr);
		mFilesVr = (Button) findViewById(R.id.files_vr);
		
		mFilesVr.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				testFilesVr();
			}
		});
		
		mPcmVr.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				testPcmVr();
			}
		});
		
		
		mSpeaker.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
				testOtherLanguage();

			}
		});

		mPcmRecorderBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
				testPcmRecoder();
			}
		});

		mPcmPlayerBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				testPcmPlayer();
			}
		});

		mSpxTts.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
				testSpxTts();
			}
		});

		mPcmTts.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
				testPcmTts();
			}
		});


	}

	protected void testFilesVr() {
		
		try {
			String accept_language = URLEncoder.encode("en_US");
			String audio_content_type = URLEncoder.encode("audio/x-wav;codec=pcm;bit=16;rate=16000");
			boolean multiple_results = true;
			String content_type = "binary/octet-stream";
			
			HttpClient client = new DefaultHttpClient();
			StringBuffer url = new StringBuffer();
			
			url.append(BASE_URL).append(STT_SERVICE).append("?")
					.append("device_id=").append(device_id)
					.append("&audio_content_type=").append(audio_content_type)
					.append("&accept_language=").append(accept_language)
					.append("&multiple_results=").append(multiple_results);

			Log.d("TAG", url.toString());

			HttpPost request = new HttpPost(url.toString());
			request.setHeader("mip-id", MIP_ID);
			request.setHeader("Hu-Id", HU_ID);
			request.setHeader("Content-Type", content_type);
			
            InputStreamEntity reqEntity = new InputStreamEntity(this.getAssets().open("Long.wav"), -1);
            reqEntity.setContentType("binary/octet-stream");
            reqEntity.setChunked(true);
            
            request.setEntity(reqEntity);
			
			// ByteArrayOutputStream baos = new ByteArrayOutputStream();
			// ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			
			HttpResponse response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			Log.e("TAG", "statusCode = "+statusCode);
			
			
			HttpEntity entity = response.getEntity();
			if(entity!=null){
				InputStream content = entity.getContent();
				int count = 0;
				byte[] buffer = new byte[1024];
				while((count = content.read(buffer))!=-1){
					Log.d("TAG", new String(buffer,0,count));
				}
				
			}
			
			
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	protected void testSpxTts() {
		


		new Thread() {

			@Override
			public void run() {

				String spxBasePath = basePath + "/spx";
				File pcmFolder = new File(spxBasePath);
				if (!pcmFolder.exists()) {
					pcmFolder.mkdir();
				}

				FileOutputStream resultWriter = null;
				try {
					File result = new File(spxBasePath
							+ "/spx_result.csv");
					if (!result.exists()) {
						result.createNewFile();
					}
					resultWriter = new FileOutputStream(result);
					String header = "Text"
							+ ","
							+ "Avg Request Start to Response Start( Second )"
							+ ","
							+ "Avg Response Start to Response Stop( Second )"
							+ "," + "Audio Size( KB )" + "\n";
					resultWriter.write(header.getBytes());
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				for (int i = 0; i < Test_Text.length; i++) {

					double avgRequestToResponse = 0;
					double avgResponseToEnd = 0;
					double avgAudioSize = 0;

					for (int j = 0; j < Times; j++) {
						// Log.d("TAG", device_id);
						// String accpet_format =
						// URLEncoder.encode("audio/x-wav;codec=pcm;bit=16;rate=16000");
						String accpet_format = URLEncoder
								.encode("audio/x-speex;rate=16000");
						Log.d("TAG", accpet_format);
						// Log.d("TAG", Accept_Format);
						//device_id = "DA2.2012345678901";
						HttpClient client = new DefaultHttpClient();
						try {
							StringBuffer url = new StringBuffer();
							url.append(BASE_URL).append(TTS_SERVICE)
									.append("?").append("device_id=")
									.append(device_id)
									.append("&language=")
									.append(Language)
									.append("&accept_format=")
									.append(accpet_format);

							Log.d("TAG", url.toString());

							HttpPost request = new HttpPost(url.toString());
							request.setHeader("mip-id", MIP_ID);
							request.setHeader("Hu-Id", HU_ID);
							request.setHeader("Content-Type",
									Content_Type);

							request.setEntity(new StringEntity(
									Test_Text[i], "UTF-8"));
							long requestStart = System
									.currentTimeMillis();

							HttpResponse response = client
									.execute(request);

							long responseStart = System
									.currentTimeMillis();
							long responseEnd = 0;
							long responseLength = 0;

							int statusCode = response.getStatusLine()
									.getStatusCode();
							Log.d("TAG", "status = " + statusCode);

							HttpEntity entity = response.getEntity();
							if (entity != null) {

								InputStream content = entity
										.getContent();

								// OggPlayer oggPlayer = new
								// OggPlayer(content);
								// new Thread(oggPlayer).start();
								// oggPlayer.setPlaying(true);

								File file = new File(spxBasePath + "/Response" + (i + 1) + "_" + (j + 1) + ".ogg");
								if (!file.exists()) {
									file.createNewFile();
								}
								FileOutputStream fos = new FileOutputStream( file);

								int count = 0;
								byte[] buf = new byte[8092];
								while ((count = content.read(buf)) != -1) {
									fos.write(buf, 0, count);
									responseLength += count;
								}
								fos.flush();
								fos.close();
								responseEnd = System
										.currentTimeMillis();
							}

							avgRequestToResponse += (double) (responseStart - requestStart) / 1000;
							avgResponseToEnd += (double) (responseEnd - responseStart) / 1000;
							avgAudioSize += responseLength/1024;

						} catch (ClientProtocolException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							client.getConnectionManager()
									.closeExpiredConnections();
						}
					}

					try {
						String record = Test_Text[i] + ","
								+ avgRequestToResponse / Times + ","
								+ avgResponseToEnd / Times + ","
								+ avgAudioSize / Times + "\n";
						resultWriter.write(record.getBytes());
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
				
				try {
					if (resultWriter != null) {
						resultWriter.flush();
						resultWriter.close();
						resultWriter = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				Log.d("TAG", "Test Spx Finished!");
			}

		}.start();
		

	
		
	}

	protected void testPcmTts() {
		

		
		
		new Thread() {

			@Override
			public void run() {

				String spxBasePath = basePath + "/pcm";
				File pcmFolder = new File(spxBasePath);
				if (!pcmFolder.exists()) {
					pcmFolder.mkdir();
				}

				FileOutputStream resultWriter = null;
				try {
					File result = new File(spxBasePath + "/pcm_result.csv");
					if (!result.exists()) {
						result.createNewFile();
					}
					resultWriter = new FileOutputStream(result);
					String header = "Text"
							+ ","
							+ "Avg Request Start to Response Start( Second )"
							+ ","
							+ "Avg Response Start to Response Stop( Second )"
							+ "," + "Audio Size( KB )" + "\n";
					resultWriter.write(header.getBytes());
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				for (int i = 0; i < Test_Text.length; i++) {
					
					double avgRequestToResponse = 0;
					double avgResponseToEnd = 0;
					double avgAudioSize = 0;

					for (int j = 0; j < Times; j++) {
						// Log.d("TAG", device_id);
						 String accpet_format =
						 URLEncoder.encode("audio/x-wav;codec=pcm;bit=16;rate=16000");
						// String accpet_format = URLEncoder
						//	.encode("audio/x-speex;rate=16000");
						Log.d("TAG", accpet_format);
						// Log.d("TAG", Accept_Format);

						HttpClient client = new DefaultHttpClient();
						try {
							StringBuffer url = new StringBuffer();
							url.append(BASE_URL).append(TTS_SERVICE)
									.append("?").append("device_id=")
									.append(device_id)
									.append("&language=")
									.append(Language)
									.append("&accept_format=")
									.append(accpet_format);

							Log.d("TAG", url.toString());

							HttpPost request = new HttpPost(url
									.toString());
							request.setHeader("mip-id", MIP_ID);
							request.setHeader("hu-id", HU_ID);
							request.setHeader("Content-Type",Content_Type);

							request.setEntity(new StringEntity(
									Test_Text[i], "UTF-8"));
							long requestStart = System
									.currentTimeMillis();

							HttpResponse response = client
									.execute(request);

							long responseStart = System
									.currentTimeMillis();
							long responseEnd = 0;
							long responseLength = 0;

							int statusCode = response.getStatusLine()
									.getStatusCode();
							Log.d("TAG", "status = " + statusCode);

							HttpEntity entity = response.getEntity();
							if (entity != null) {

								InputStream content = entity
										.getContent();

								// OggPlayer oggPlayer = new
								// OggPlayer(content);
								// new Thread(oggPlayer).start();
								// oggPlayer.setPlaying(true);

								File file = new File(spxBasePath
										+ "/Response" + (i + 1) + "_"
										+ (j + 1) + ".pcm");
								if (!file.exists()) {
									file.createNewFile();
								}
								FileOutputStream fos = new FileOutputStream(
										file);

								int count = 0;
								byte[] buf = new byte[8092];
								while ((count = content.read(buf)) != -1) {
									fos.write(buf, 0, count);
									responseLength += count;
								}
								fos.flush();
								fos.close();
								responseEnd = System.currentTimeMillis();
							}

							avgRequestToResponse += (double) (responseStart - requestStart) / 1000;
							avgResponseToEnd += (double) (responseEnd - responseStart) / 1000;
							avgAudioSize += responseLength/1024;

						} catch (ClientProtocolException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							client.getConnectionManager()
									.closeExpiredConnections();
						}
					}

					try {
						String record = Test_Text[i] + ","
								+ avgRequestToResponse / Times + ","
								+ avgResponseToEnd / Times + ","
								+ avgAudioSize / Times + "\n";
						resultWriter.write(record.getBytes());
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
				
				try {
					if (resultWriter != null) {
						resultWriter.flush();
						resultWriter.close();
						resultWriter = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				Log.d("TAG", "Test Spx Finished!");
			}

		}.start();
		

	
		
	}

	protected void testPcmPlayer() {
		

		mPcmPlayer = new PcmPlayer();
		new Thread(mPcmPlayer).start();
		mPcmPlayer.setPlaying(true);

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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

	protected void testPcmRecoder() {

		mPcmRecorder = new PcmRecorder();
		new Thread(mPcmRecorder).start();
		mPcmRecorder.setRecording(true);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		ProgressBar pb = new ProgressBar(MainActivity.this);
		builder.setTitle("Please Speaking");
		builder.setView(pb);
		builder.setPositiveButton("Finish",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (mPcmRecorder.isRecording()) {
							mPcmRecorder.setRecording(false);
						}
					}
				});
		builder.show();

	}

	protected void testOtherLanguage() {
		
		
		new Thread() {

			@Override
			public void run() {

				// Log.d("TAG", device_id);
				String accpet_format = URLEncoder.encode("audio/x-wav;codec=pcm;bit=16;rate=16000");
				//String accpet_format = URLEncoder.encode("audio/x-speex;rate=16000");
				Log.d("TAG", accpet_format);
				
				// Log.d("TAG", Accept_Format);
				
				
//				 String language = "jp_JP";
//				 String text = "¤ªÔªšÝ¤Ç¤¹¤«¡£";
				
				//Long Text
//				 String language = "en_US";
//				 String text = " In this case, the sending device can send up to 5 TCP Segments without receiving an acknowledgement from the receiving device. After receiving the acknowledgement for Segment 1 from the receiving device, the sending device can slide its window one TCP Segment to the right side and the sending device can transmit segment 6 also.If any TCP Segment lost while its journey to the destination, the receiving device cannot acknowledge the sender. Consider while transmission, all other Segments reached the destination except Segment 3. The receiving device can acknowledge up to Segment 2. At the sending device, a timeout will occur and it will re-transmit the lost Segment 3. Now the receiving device has received all the Segments, since only Segment 3 was lost. Now the receiving device will send the ACK for Segment 5, because it has received all the Segments to Segment 5.Acknowledgement for Segment 5 ensures the sender the receiver has succesfully received all the Segments up to 5.TCP uses a byte level numbering system for communication. If the sequence number for a TCP segment at any instance was 5000 and the Segment carry 500 bytes, the sequence number for the next Segment will be 5000+500+1. That means TCP segment only carries the sequence number of the first byte in the segment.The Window size is expressed in number of bytes and is determined by the receiving device when the connection is established and can vary later. You might have noticed when transferring big files from one Windows machine to another, initially the time remaining calculation will show a large value and will come down later.";
				
				 String language = "en_US";
				 String text = "A daily dose of vitamin D could actually increase a man's risk of prostate cancer a study out today shows researchers discovered the disturbing link while studying the effects of antioxidants on men's health";
				
				 
			
				Log.d("Test Data", text);
				Log.d("Test Data", "length = "+text.length());
				
				HttpClient client = new DefaultHttpClient();
				try {
					StringBuffer url = new StringBuffer();
					url.append(BASE_URL).append(TTS_SERVICE).append("?")
						.append("device_id=").append(device_id)
						.append("&language=").append(language)
						.append("&accept_format=").append(accpet_format);

					Log.d("TAG", url.toString());

					HttpPost request = new HttpPost(url.toString());
					request.setHeader("mip-id", MIP_ID);
					request.setHeader("Hu-Id", HU_ID);
					request.setHeader("Content-Type", Content_Type);
					request.setEntity(new StringEntity(text, "UTF-8"));
					HttpResponse response = client.execute(request);
					int statusCode = response.getStatusLine().getStatusCode();
					
					Log.d("TAG", "status = " + statusCode);

					HttpEntity entity = response.getEntity();
					if (statusCode == HttpStatus.SC_OK && entity != null) {

						InputStream content = entity.getContent();
						
//						OggPlayer oggPlayer = new OggPlayer(content);
//						new Thread(oggPlayer).start();
//						oggPlayer.setPlaying(true);
						
						
						mPcmPlayer = new PcmPlayer(content);
						new Thread(mPcmPlayer).start();
						mPcmPlayer.setPlaying(true);


					}else{
						Log.d("TAG", "Error Message");

						InputStream content = entity.getContent();

						byte[] b = new byte[1024];
						int c = 0;
						while ((c = content.read(b)) != -1) {
							Log.e("TAG", new String(b,0,c));
						}
					}

				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					client.getConnectionManager().closeExpiredConnections();
				}
				Log.d("TAG", "Test Spx Finished!");
			}
		}.start();
		
	}
	
	protected void testPcmVr() {
		
		mPcmRecorderWithVad = new PcmRecorderWithVad();
		new Thread(mPcmRecorderWithVad).start();
		mPcmRecorderWithVad.setRecording(true);
		
		
		
//		try {
//			String accept_language = URLEncoder.encode("en_US");
//			String audio_content_type = URLEncoder.encode("audio/x-wav;codec=pcm;bit=16;rate=16000");
//			boolean multiple_results = true;
//			String content_type = "binary/octet-stream";
//			
//			HttpClient client = new DefaultHttpClient();
//			StringBuffer url = new StringBuffer();
//			
//			url.append(BASE_URL).append(STT_SERVICE).append("?")
//					.append("device_id=").append(device_id)
//					.append("&audio_content_type=").append(audio_content_type)
//					.append("&accept_language=").append(accept_language)
//					.append("&multiple_results=").append(multiple_results);
//
//			Log.d("TAG", url.toString());
//
//			HttpPost request = new HttpPost(url.toString());
//			request.setHeader("mip-id", MIP_ID);
//			request.setHeader("Hu-Id", HU_ID);
//			request.setHeader("Content-Type", content_type);
//			
//			//request.setEntity(new StringEntity(text, "UTF-8"));
//			//request.getEntity().get
//			
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//			
//			HttpResponse response = client.execute(request);
//			int statusCode = response.getStatusLine().getStatusCode();
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		ProgressBar pb = new ProgressBar(MainActivity.this);
		builder.setTitle("Please Speaking");
		builder.setView(pb);
		builder.setPositiveButton("Finish",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (mPcmRecorderWithVad.isRecording()) {
							mPcmRecorderWithVad.setRecording(false);
						}
					}
				});
		builder.show();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
