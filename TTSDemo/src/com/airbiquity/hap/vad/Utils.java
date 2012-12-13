package com.airbiquity.hap.vad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {
	
	public static byte[] convertShortArrayToByteArray(short[] srcArr, int offset, int length){
		
		ByteBuffer buf = ByteBuffer.allocate(length*2);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		for(int i = offset; i < offset + length;i++){
			buf.putShort(srcArr[i]);
		}
		
		return buf.array();
	}

}
