package com.airbiquity.hap.tts;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

public class Recorder implements Runnable {
	
	private static final int sampleRateInHz = 16000;
	private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	
	private final Object mutex = new Object();
	private volatile boolean isRecording = false;
	private DataOutputStream dataOutputStreamInstance = null;

	public Recorder() {
		
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

		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		//Initialize AudioRecord, make it ready for record.
		int bufferRead = 0;
		int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioEncoding);
		byte[] tempBuffer = new byte[bufferSize];
		AudioRecord recordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioEncoding, bufferSize*2);
		recordInstance.startRecording();
		
		
		while (this.isRecording) {

			bufferRead = recordInstance.read(tempBuffer, 0, bufferSize);
			
			if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException( "read() returned AudioRecord.ERROR_INVALID_OPERATION");
			} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
				throw new IllegalStateException( "read() returned AudioRecord.ERROR_BAD_VALUE");
			} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException( "read() returned AudioRecord.ERROR_INVALID_OPERATION");
			}
			
			try {
				dataOutputStreamInstance.write(tempBuffer, 0, bufferRead);
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
