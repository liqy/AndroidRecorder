package com.airbiquity.hap.tts;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class PcmWriter implements Runnable{
	private final Object mutex = new Object();
	private volatile boolean isPlaying = false;
	private RawData rawData;
	private List<RawData> list;
	
	private static final int sampleRateInHz = 16000;
	private static final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	
	private AudioTrack audioTrack = null;

	public PcmWriter() {
		super();
		list = Collections.synchronizedList(new LinkedList<RawData>());
		init();
	}
	
	public void init(){
		int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioEncoding);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, audioEncoding, 2 * bufferSizeInBytes, AudioTrack.MODE_STREAM);
		audioTrack.play();
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
		
		while (this.isPlaying()) {
			if (list.size() > 0) {
				rawData = list.remove(0);
				audioTrack.write(rawData.pcm,0,rawData.size);
			} else {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		stop();
	}

	public void putData(byte[] buf, int size) {
		RawData data = new RawData();
		data.size = size;
		System.arraycopy(buf, 0, data.pcm, 0, size);
		list.add(data);
	}

	public void stop() {
		audioTrack.stop();
	}

	public void setPlaying(boolean isRecording) {
		synchronized (mutex) {
			this.isPlaying = isRecording;
			if (this.isPlaying) {
				mutex.notify();
			}
		}
	}

	public boolean isPlaying() {
		synchronized (mutex) {
			return this.isPlaying;
		}
	}
	
	class RawData {
		int size;
		byte[] pcm = new byte[8092];
	}
	
}
