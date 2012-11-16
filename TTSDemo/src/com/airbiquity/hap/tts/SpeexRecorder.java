package com.airbiquity.hap.tts;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xiph.speex.SpeexEncoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

public class SpeexRecorder implements Runnable {

	private static int mode = 1;
	private static int quality = 8;
	// private static int complexity = 3;
	// private static int bitrate = -1;
	// private static float vbr_quality = -1;
	// private static boolean vbr = false;
	// private static boolean vad = false;
	// private static boolean dtx = false;
	// private static int channels = 1;

	private static final int sampleRate = 16000;
	private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private final Object mutex = new Object();
	private volatile boolean isRecording = false;
	private DataOutputStream dataOutputStreamInstance = null;

	// private OggSpeexWriter oggSpxWriter = null;
	private SpeexEncoder speexEncorder = null;

	public SpeexRecorder() {

		try {
			File pcmFile = new File(Environment.getExternalStorageDirectory()
					+ "/test.spx");
			if (pcmFile.exists()) {
				pcmFile.delete();
				pcmFile.createNewFile();
			}
			// Get output stream from file, ready to write out the audio
			// BufferedOutputStream bufferedStreamInstance = null;
			// bufferedStreamInstance = new BufferedOutputStream(new
			// FileOutputStream(pcmFile));
			 dataOutputStreamInstance = new DataOutputStream(new FileOutputStream(pcmFile));

			// oggSpxWriter = new OggSpeexWriter();
			// oggSpxWriter.open(Environment.getExternalStorageDirectory() +
			// "/test.spx");
			// oggSpxWriter.setFormat(mode, sampleRate, channelConfig);

			speexEncorder = new SpeexEncoder();

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

		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		// Initialize AudioRecord, make it ready for record.
		int bufferRead = 0;
		int bufferSize = AudioRecord.getMinBufferSize(sampleRate,
				channelConfig, audioEncoding);
		byte[] tempBuffer = new byte[bufferSize];
		AudioRecord recordInstance = new AudioRecord( MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioEncoding, bufferSize * 2);
		recordInstance.startRecording();

		// construct a new encoder
		// SpeexEncoder speexEncoder = new SpeexEncoder();
		// mode = 0;--narrowband
		// mode = 1;--wideband
		// mode = 2;--ultra-wideband

		// quality = Encoding quality (0-10) default 8");

		// samplingRate = 8000;
		// samplingRate = 16000;
		// samplingRate = 32000;

		// channels = 1; default
		// channels = 2; Consider input as stereo
		// Log.d("TAD",
		// "mode = "+mode+"quality = "+quality+"samplingRate = "+samplingRate+"channels = "+channels);
		speexEncorder.init(mode, quality, sampleRate, 1);

		// complexity = Encoding complexity (0-10) default 3
		// if (complexity > 0) {
		// speexEncoder.getEncoder().setComplexity(complexity);
		// }
		// // Sets the bitrate.
		// if (bitrate > 0) {
		// speexEncoder.getEncoder().setBitRate(bitrate);
		// }
		// if (vbr) {
		// speexEncoder.getEncoder().setVbr(vbr);
		// if (vbr_quality > 0) {
		// speexEncoder.getEncoder().setVbrQuality(vbr_quality);
		// }
		// }
		// if (vad) {
		// speexEncoder.getEncoder().setVad(vad);
		// }
		//
		// if (dtx) {
		// speexEncoder.getEncoder().setDtx(dtx);
		// }

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
				speexEncorder.processData(tempBuffer, 0, bufferRead);
				bufferRead = speexEncorder.getProcessedData(tempBuffer, 0);
				
				// write speex to the file
				dataOutputStreamInstance.write(tempBuffer, 0, bufferRead);
				// oggSpxWriter.writePacket(tempBuffer, 0, bufferRead);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		recordInstance.stop();

		// try {
		// oggSpxWriter.flush(true);
		// oggSpxWriter.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

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
