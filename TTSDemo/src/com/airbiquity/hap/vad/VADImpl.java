package com.airbiquity.hap.vad;

public class VADImpl {
	/** Constants **/
	private int AUDIO_BUFFER_SIZE = 320;
	private int MAX_NUM_BUFFERS = 32768;
	private int HANGOVER_ACTIVE = 8000; // # of samples
	private int HANGOVER_INACTIVE = 320; // # of samples
	private long THRESHOLD_FACTOR = 0x00008CCD; // 1.1 in Q15 format

	public VADImpl() {
	}

	public void init() {
		System.out.println("SimpleVAD init...");
		VADEntity.VAD_MaxAbsMean = 0;
		VADEntity.VAD_BufferCount = 0;
		VADEntity.VAD_HystCount = -HANGOVER_INACTIVE;
	}

	static class VADEntity {
		public static int VAD_MaxAbsMean;
		public static int VAD_BufferCount;
		public static int VAD_HystCount;
	}

	public boolean updateVadStatus(byte[] srcPtr, int length) {

		short bufferMaxAbs = 0;
		int threshold = 0;
		boolean isBufferVoice;

		/* find the maximum absolute value in the current buffer */
		bufferMaxAbs = getMaxAbs(srcPtr, length);
		// System.out.println("bufferMaxAbs=="+bufferMaxAbs);
		/* update the average and compute the threshold */
		VADEntity.VAD_MaxAbsMean = (VADEntity.VAD_MaxAbsMean * VADEntity.VAD_BufferCount + (int) bufferMaxAbs) / (VADEntity.VAD_BufferCount + 1);

		threshold = (int) ((VADEntity.VAD_MaxAbsMean * THRESHOLD_FACTOR) >> 15);
		// System.out.println("threshold=="+threshold);
		if (VADEntity.VAD_BufferCount < MAX_NUM_BUFFERS) {
			VADEntity.VAD_BufferCount++;
		}

		/* compare buffer energy to threshold */
		if (bufferMaxAbs > threshold) {
			isBufferVoice = true;
		} else {
			isBufferVoice = false;
		}
		/* Current state is INACTIVE */
		if (VADEntity.VAD_HystCount <= 0) {
			if (isBufferVoice) {
				VADEntity.VAD_HystCount += AUDIO_BUFFER_SIZE;

				if (VADEntity.VAD_HystCount > 0) {
					/* change state to ACTIVE */
					VADEntity.VAD_HystCount = HANGOVER_ACTIVE;
				}
			} else {
				VADEntity.VAD_HystCount = -HANGOVER_INACTIVE;
			}
		} else {/* Current state is ACTIVE */
			if (!isBufferVoice) {
				VADEntity.VAD_HystCount -= AUDIO_BUFFER_SIZE;

				if (VADEntity.VAD_HystCount < 0) {
					/* change state to INACTIVE */
					VADEntity.VAD_HystCount = -HANGOVER_INACTIVE;
				}
			} else {
				VADEntity.VAD_HystCount = HANGOVER_ACTIVE;
			}
		}
		System.out.println("VADEntity.VAD_HystCount==" + VADEntity.VAD_HystCount);
		if (VADEntity.VAD_HystCount <= 0) {
			return false;
		} else {
			return true;
		}

	}

	/**************************************************************************
	 * Function Name: aq_MaxAbs
	 * 
	 * Description: maximum absolute value
	 * 
	 * Input Variables: byte[] srcPtr -- input buffer int length -- number of
	 * input values
	 * 
	 * Output Variables: int destPtr -- maximum absolute value.
	 * 
	 * Return Value: index of the element with the greatest absolute value
	 * IMPORTANT: If there are multiple occurrences of the maximum, the smallest
	 * index will be returned.
	 *************************************************************************/
	/**
	 * 
	 * @param destPtr
	 * @param srcPtr
	 * @param length
	 * @return
	 */
	public short getMaxAbs(byte[] srcPtr, int length) {
		short max = 0;
		short[] shortValue = byteToShort(srcPtr, length / 2);
		for (int j = 0; j < shortValue.length; j++) {
			max = (short) (StrictMath.abs(shortValue[j]) > max ? StrictMath.abs(shortValue[j]) : max);
		}
		return max;
	}

	// get short from byte
	public short[] byteToShort(byte[] byteBuffer, int length) {
		short[] shortBuffer = new short[length];
		short r = 0;
		for (int i = 0, a = 0; i < byteBuffer.length; i = i + 2, a++) {
			r = (short) (byteBuffer[i + 1] & 0xff | (byteBuffer[i] & 0xff) << 8);
			shortBuffer[a] = r;
		}
		return shortBuffer;
	}
}
