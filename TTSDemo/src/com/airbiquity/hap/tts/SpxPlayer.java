package com.airbiquity.hap.tts;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.xiph.speex.SpeexDecoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;

public class SpxPlayer implements Runnable{
	
	private static final int mode = 1;
	private static final int sampleRateInHz = 16000;
	private static final int channels = 1;
	
	private static final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	
	private AudioTrack audioTrack = null;
	private DataInputStream dataInputStreamInstance = null;
	
	private final Object mutex = new Object();
	private volatile boolean isPlaying = false;
	
	private SpeexDecoder spxDecoder = null;
	
	public SpxPlayer(){
		try {
			File pcmFile = new File(Environment.getExternalStorageDirectory() + "/test.spx");
			dataInputStreamInstance = new DataInputStream(new FileInputStream(pcmFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		spxDecoder = new SpeexDecoder();
	}
	
	public SpxPlayer(InputStream is){
		this.dataInputStreamInstance = new DataInputStream(is);
	}

	public void run() {
		
		synchronized (mutex) {
			while (!this.isPlaying) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					throw new IllegalStateException("Wait() interrupted!", e);
				}
			}
		}
		
		spxDecoder.init(mode, sampleRateInHz, channels,true);
		
		int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioEncoding);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, audioEncoding, 2 * bufferSizeInBytes, AudioTrack.MODE_STREAM);
		audioTrack.play();

		try {
			
			byte[] buffer = new byte[bufferSizeInBytes];
			int count = 0;
			while ((count = dataInputStreamInstance.read(buffer)) != -1) {
				spxDecoder.processData(buffer, 0, count);
				byte[] decodedBuffer = new byte[spxDecoder.getProcessedDataByteSize()];
				int decodedCount = spxDecoder.getProcessedData(decodedBuffer, 0);
				audioTrack.write(decodedBuffer, 0, decodedCount);
			}
			dataInputStreamInstance.close();
			

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			audioTrack.stop();
			dataInputStreamInstance = null;
		}

	}
	
	public void setPlaying(boolean isPlaying) {
		synchronized (mutex) {
			this.isPlaying = isPlaying;
			if (this.isPlaying) {
				mutex.notify();
			}else{
				audioTrack.stop();
			}
		}
	}

	public boolean isPlaying() {
		synchronized (mutex) {
			return isPlaying;
		}
	}
}
