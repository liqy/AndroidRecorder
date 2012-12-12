package com.airbiquity.hap.tts;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.airbiquity.hap.speex.OggSpeexWriter;
import com.airbiquity.hap.speex.PcmWaveWriter;
import com.airbiquity.hap.speex.SpeexEncoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

public class SpeexRecorder implements Runnable {

	private static int mode = 1;
	private static int quality = 8;
	private static int channels = 1;

	private static final int sampleRate = 16000;
	private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private final Object mutex = new Object();
	private volatile boolean isRecording = false;
	
	private DataOutputStream dataOutputStreamInstance = null;
	private OggSpeexWriter oggSpxWriter = null;
	private PcmWaveWriter pcmWriter =null;
	
	private SpeexEncoder spxEncoder = null;

	public SpeexRecorder() {

		try {
			File pcmFile = new File(Environment.getExternalStorageDirectory() + "/test.spx");
			if (pcmFile.exists()) {
				pcmFile.delete();
				pcmFile.createNewFile();
			}
			
			// Get output stream from file, ready to write out the audio
			// BufferedOutputStream bufferedStreamInstance = null;
			// bufferedStreamInstance = new BufferedOutputStream(new
			// FileOutputStream(pcmFile));
			dataOutputStreamInstance = new DataOutputStream(new FileOutputStream(pcmFile));
			
			pcmWriter = new PcmWaveWriter(16000,1);
			pcmWriter.open(Environment.getExternalStorageDirectory() + "/test.wav");
			
			oggSpxWriter = new OggSpeexWriter(1,16000,1,1,true);
			oggSpxWriter.open(Environment.getExternalStorageDirectory() + "/test.ogg");

			// oggSpxWriter = new OggSpeexWriter();
			// oggSpxWriter.open(Environment.getExternalStorageDirectory() +
			// "/test.spx");
			// oggSpxWriter.setFormat(mode, sampleRate, channelConfig);
			spxEncoder = new SpeexEncoder();

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

		// Initialize AudioRecord, make it ready for record.
		int bufferRead = 0;
		int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);
		byte[] tempBuffer = new byte[bufferSize];
		AudioRecord recordInstance = new AudioRecord( MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioEncoding, bufferSize * 2);
		recordInstance.startRecording();

		spxEncoder.init(mode, quality, 44100, channels);
		try {
			pcmWriter.writeHeader("pcm audio");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			oggSpxWriter.writeHeader("ogg speex");
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// try {
		// oggSpxWriter.writeHeader();
		// } catch (IOException e1) {
		// e1.printStackTrace();
		// }
		
		while (this.isRecording) {

			// read data from device
			bufferRead = recordInstance.read(tempBuffer, 0, bufferSize);

			if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_BAD_VALUE");
			} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			}
			
			

			try {
				
				// encode with speex
//				Log.d("--->", "bufferRead--->" + bufferRead);
				spxEncoder.processData(tempBuffer, 0,bufferRead);
				byte[] encodedBuffer = new byte[spxEncoder.getProcessedDataByteSize()];
//				Log.d("--->",
//						"getProcessedDataByteSize--->"
//								+ spxEncoder.getProcessedDataByteSize());
				int encodedCount = spxEncoder.getProcessedData(encodedBuffer, 0);
//				Log.d("--->", "encodedCount--->" + encodedCount);
//				dataOutputStreamInstance.write(encodedBuffer, 0, encodedCount);
			
				dataOutputStreamInstance.write(tempBuffer, 0, bufferRead);
				
				// write wav file
				pcmWriter.writePacket(tempBuffer, 0	, bufferRead);
				
				// write speex to the file
			    oggSpxWriter.writePacket(encodedBuffer, 0, encodedCount);
				

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		recordInstance.stop();

		 try {
			 pcmWriter.close();
			 oggSpxWriter.close();
		 } catch (IOException e) {
			 e.printStackTrace();
		 }

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
