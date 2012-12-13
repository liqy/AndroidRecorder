package com.airbiquity.hap.tts;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.airbiquity.hap.vad.SimpleVAD;
import com.airbiquity.hap.vad.Utils;

public class PcmRecorderWithVad implements Runnable {
	
	private static final int sampleRateInHz = 16000;
	private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	
	private final Object mutex = new Object();
	private volatile boolean isRecording = false;
	private DataOutputStream dataOutputStreamInstance = null;

	public PcmRecorderWithVad() {
		
		try {
			File pcmFile = new File(Environment.getExternalStorageDirectory() + "/test.pcm");
			if (pcmFile.exists()) {
				pcmFile.delete();
				pcmFile.createNewFile();
			}
			// Get output stream from file, ready to write out the audio
			BufferedOutputStream bufferedStreamInstance = null;
			bufferedStreamInstance = new BufferedOutputStream( new FileOutputStream( pcmFile ) );

			dataOutputStreamInstance = new DataOutputStream(bufferedStreamInstance);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void run() {
		
		synchronized (mutex) {
			while (!this.isRecording) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					throw new IllegalStateException("Wait() interrupted!", e);
				}
			}
		}
		
		HttpURLConnection urlConnection = null;
		OutputStream out = null;
		InputStream in = null;
		try {
			URL url = new URL("http://192.168.1.101:8080/ChunkServer/ChunkToast");
			urlConnection = (HttpURLConnection) url.openConnection();
			
			urlConnection.setRequestProperty("mip-Id", "1234");
			urlConnection.setRequestProperty("huId", "1234567890");
			
			
			urlConnection.setDoOutput(true);
			urlConnection.setChunkedStreamingMode(0);

			out = new BufferedOutputStream(urlConnection.getOutputStream());
			//writeStream(out);
			
			in = new BufferedInputStream(urlConnection.getInputStream());
			//readStream(in);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		//Initialize AudioRecord, make it ready for record.
		int bufferRead = 0;
		int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioEncoding);
		short[] tempBuffer = new short[bufferSize/2];
		AudioRecord recordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioEncoding, bufferSize*2);
		recordInstance.startRecording();
		
		
		
		
		SimpleVAD vadImpl = new SimpleVAD();
		
		while (this.isRecording) {

			bufferRead = recordInstance.read(tempBuffer, 0, bufferSize/2);
			
			if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException( "read() returned AudioRecord.ERROR_INVALID_OPERATION");
			} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
				throw new IllegalStateException( "read() returned AudioRecord.ERROR_BAD_VALUE");
			} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException( "read() returned AudioRecord.ERROR_INVALID_OPERATION");
			}
			
			boolean updateVadStatus = vadImpl.update(tempBuffer, bufferRead);
			Log.d("TAG", "is voice detected = "+updateVadStatus);
			
			//TODO write it to file
			try {
				byte[] buf = Utils.convertShortArrayToByteArray(tempBuffer, 0, bufferRead);
				dataOutputStreamInstance.write(buf, 0, buf.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		recordInstance.stop();
		
		try {
			dataOutputStreamInstance.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setRecording(boolean isRecording) {
		synchronized (mutex) {
			this.isRecording = isRecording;
			if (this.isRecording) {
				mutex.notify();
			}
		}
	}

	public boolean isRecording() {
		synchronized (mutex) {
			return isRecording;
		}
	}

}
