package com.airbiquity.hap.tts;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class PcmPlayer implements Runnable{
	
	private static final int sampleRateInHz = 16000;
	private static final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	
	private AudioTrack audioTrack = null;
	private DataInputStream dataInputStreamInstance = null;
	
	private final Object mutex = new Object();
	private volatile boolean isPlaying = false;
	
	public PcmPlayer(){
		// try {
		// File pcmFile = new File(Environment.getExternalStorageDirectory() +
		// "/test.pcm");
		// dataInputStreamInstance = new DataInputStream(new
		// FileInputStream(pcmFile));
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// }
	}
	
	public PcmPlayer(InputStream is){
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
		
		int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioEncoding);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, audioEncoding, 2 * bufferSizeInBytes, AudioTrack.MODE_STREAM);
		audioTrack.play();
		
		try {
			
			byte[] buffer = new byte[8092];
			int all = 0;
			int count = 0;
			while ((count = dataInputStreamInstance.read(buffer)) != -1) {
			    Log.d("--->", "count--->"+count);
			    Log.d("--->",  new String(buffer,0,count));
			    all += count;
				audioTrack.write(buffer,0,count);
			}
			Log.d("--->", "all--->"+all);
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
