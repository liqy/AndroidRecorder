package com.airbiquity.hap.vad;

public class SimpleVAD {

	private static final int MAX_NUM_BUFFERS = 32768;
	private static final int HANGOVER_ACTIVE = 6000; // # of samples
	private static final int HANGOVER_INACTIVE = 320;// # of samples
	private static final int THRESHOLD_FACTOR = 0x00008CCD; // 1.1 in Q15 format
	public static final int AUDIO_BUFFER_SIZE = 320;

	public SimpleVAD() {
		VADEntity.VAD_MaxAbsMean = 0;
		VADEntity.VAD_BufferCount = 0;
		VADEntity.VAD_HystCount = -HANGOVER_INACTIVE;
	}

	public boolean update(short[] srcBuffer, int length) {

		short bufferMaxAbs;
		int threshold;
		boolean isBufferVoice;

		bufferMaxAbs = getMaxAbs(srcBuffer, length);

		/* update the average and compute the threshold */
		VADEntity.VAD_MaxAbsMean = (VADEntity.VAD_MaxAbsMean
				* VADEntity.VAD_BufferCount + bufferMaxAbs)
				/ (VADEntity.VAD_BufferCount + 1);

		threshold = (VADEntity.VAD_MaxAbsMean * THRESHOLD_FACTOR) >> 15;

		if (VADEntity.VAD_BufferCount < MAX_NUM_BUFFERS) {
			VADEntity.VAD_BufferCount++;
		}

		/* compare buffer energy to threshold */
		if (bufferMaxAbs > threshold) {
			isBufferVoice = true;
		} else {
			isBufferVoice = false;
		}

		if (VADEntity.VAD_HystCount <= 0) {
			if (isBufferVoice) {
				VADEntity.VAD_HystCount += AUDIO_BUFFER_SIZE;

				if (VADEntity.VAD_HystCount > 0)
					/* change state to ACTIVE */
					VADEntity.VAD_HystCount = HANGOVER_ACTIVE;
			} else
				VADEntity.VAD_HystCount = -HANGOVER_INACTIVE;
		} else {/* Current state is ACTIVE */
			if (!isBufferVoice) {
				VADEntity.VAD_HystCount -= AUDIO_BUFFER_SIZE;

				if (VADEntity.VAD_HystCount < 0)
					/* change state to INACTIVE */
					VADEntity.VAD_HystCount = -HANGOVER_INACTIVE;
			} else
				VADEntity.VAD_HystCount = HANGOVER_ACTIVE;
		}

		if (VADEntity.VAD_HystCount <= 0) {
			return false;
		} else {
			return true;
		}
	}

	static class VADEntity {
		public static int VAD_MaxAbsMean;
		public static int VAD_BufferCount;
		public static int VAD_HystCount;
	}

	/**
	 * maximum absolute value
	 * 
	 * @param source
	 * @param length
	 * @return
	 */
	private short getMaxAbs(short[] source, int length) {

		short max = 0;
		for (int i = 0; i < length; i++) {
			max = getAbs(source[i])>max?getAbs(source[i]):max;
		}
		return max;
	}

	private short getAbs(short n) {
		int temp = n;
		temp = temp >> 31;
		n ^= temp;
		n -= temp;
		return n;
	}

}
