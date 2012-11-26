package com.airbiquity.hap.tts;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.util.Random;

import org.xiph.speex.OggCrc;
import org.xiph.speex.SpeexDecoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class OggPlayer implements Runnable{
	
	private int sampleRateInHz = 16000;
	
	private static final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	
	/** Print level for messages : Print debug information */
	public static final int DEBUG = 0;
	/** Print level for messages : Print basic information */
	public static final int INFO = 1;
	/** Print level for messages : Print only warnings and errors */
	public static final int WARN = 2;
	/** Print level for messages : Print only errors */
	public static final int ERROR = 3;
	/** Print level for messages */
	protected int printlevel = INFO;
	
	/** Random number generator for packet loss simulation. */
	protected static Random random = new Random();

	protected SpeexDecoder speexDecoder;
	private AudioTrack audioTrack = null;

	/** Defines whether or not the perceptual enhancement is used. */
	protected boolean enhanced = true;
	/** If input is raw, defines the decoder mode (0=NB, 1=WB and 2-UWB). */
	private int mode = 1;
	/** If input is raw, defines the quality setting used by the encoder. */
	private int quality = 8;
	/** If input is raw, defines the number of frmaes per packet. */
	private int nframes = 1;
	/** If input is raw, defines the sample rate of the audio. */
	private int sampleRate = -1;
	/** */
	private float vbr_quality = -1;
	/** */
	private boolean vbr = false;
	/** If input is raw, defines th number of channels (1=mono, 2=stereo). */
	private int channels = 1;
	/** The percentage of packets to lose in the packet loss simulation. */
	private int loss = 0;
	
	private final Object mutex = new Object();
	private volatile boolean isPlaying = false;
	
	private DataInputStream mDataInputStream;
	
	public OggPlayer(){
		
	}
	
	public OggPlayer(InputStream is){
		mDataInputStream = new DataInputStream(is);
	}

	public void run() {
		
		
		byte[] header = new byte[2048];
		byte[] payload = new byte[65536];
		byte[] decdat = new byte[44100 * 2 * 2];
		final int OGG_HEADERSIZE = 27;
		final int OGG_SEGOFFSET = 26;
		final String OGGID = "OggS";
		int segments = 0;
		int curseg = 0;
		int bodybytes = 0;
		int decsize = 0;
		int packetNo = 0;
		

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

		
		int origchksum;
		int chksum;
		
		try {
			
			while(isPlaying){
			      // read the OGG header
				  int a = mDataInputStream.available();
				  Log.d("--->", "avaliable  =  "+a);
				  mDataInputStream.readFully(header, 0, OGG_HEADERSIZE);
				  Log.d("TAG", "header = " + new String(header,0,OGG_HEADERSIZE));
				  
			      origchksum = readInt(header, 22);
			      Log.d("TAG", "Origin Check Sum = " + origchksum);
			      header[22] = 0;
			      header[23] = 0;
			      header[24] = 0;
			      header[25] = 0;
			      
			      chksum=OggCrc.checksum(0, header, 0, OGG_HEADERSIZE);
			      Log.d("TAG", "Check Sum = " + chksum);
			      
			      // make sure its a OGG header
			      if (!OGGID.equals(new String(header, 0, 4))) {
			    	System.err.println("missing ogg id!");
			        return;
			      }

			      /* how many segments are there? */
			      segments = header[OGG_SEGOFFSET] & 0xFF;
			      Log.d("TAG", " how many segments are there? segments = " + segments);
			      
			      mDataInputStream.readFully(header, OGG_HEADERSIZE, segments);
			      Log.d("TAG", "Segments = " + header[OGG_HEADERSIZE+segments]);
			      
			      chksum=OggCrc.checksum(chksum, header, OGG_HEADERSIZE, segments);
			      Log.d("TAG", "Check Sum = " + chksum);

			    /* decode each segment, writing output to wav */
				for (curseg = 0; curseg < segments; curseg++) {
					/* get the number of bytes in the segment */
					bodybytes = header[OGG_HEADERSIZE + curseg] & 0xFF;
					Log.d("TAG", "get the number of bytes in the segment. bodybytes= " + bodybytes);	
					
					if (bodybytes == 255) {
						System.err.println("sorry, don't handle 255 sizes!");
						return;
					}
					
					mDataInputStream.readFully(payload, 0, bodybytes);
					chksum = OggCrc.checksum(chksum, payload, 0, bodybytes);

					/* decode the segment */
					/* if first packet, read the Speex header */
					if (packetNo == 0) {
						if (readSpeexHeader(payload, 0, bodybytes)) {
							
							System.out.println("File Format: Ogg Speex");
							System.out.println("Sample Rate: " + sampleRate);
							System.out.println("Channels: " + channels);
							System.out.println("Encoder mode: " + (mode == 0 ? "Narrowband" : (mode == 1 ? "Wideband" : "UltraWideband")));
							System.out.println("Frames per packet: " + nframes);
							packetNo++;
							
						} else {
							packetNo = 0;
						}
					} else if (packetNo == 1) { // Ogg Comment packet
						packetNo++;
					} else {
						if (loss > 0 && random.nextInt(100) < loss) {
							speexDecoder.processData(null, 0, bodybytes);
							for (int i = 1; i < nframes; i++) {
								speexDecoder.processData(true);
							}
						} else {
							speexDecoder.processData(payload, 0, bodybytes);
							for (int i = 1; i < nframes; i++) {
								speexDecoder.processData(false);
							}
						}
						
						/* get the amount of decoded data */
						if ((decsize = speexDecoder.getProcessedData(decdat, 0)) > 0) {
							//writer.writePacket(decdat, 0, decsize);
							audioTrack.write(decdat, 0, decsize);
						}
						
						packetNo++;
					}
				}
				
				if (chksum != origchksum){
					throw new IOException("Ogg CheckSums do not match");
				}
				
			}
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (EOFException eof){
			
		}catch (IOException e) {
			e.printStackTrace();
		}finally{
			audioTrack.flush();
			audioTrack.stop();
			audioTrack = null;
			
			mDataInputStream = null;
		}
		
	}
	
	
	/**
	 * Reads the header packet.
	 * 
	 * <pre>
	 *  0 -  7: speex_string: "Speex   "
	 *  8 - 27: speex_version: "speex-1.0"
	 * 28 - 31: speex_version_id: 1
	 * 32 - 35: header_size: 80
	 * 36 - 39: rate
	 * 40 - 43: mode: 0=narrowband, 1=wb, 2=uwb
	 * 44 - 47: mode_bitstream_version: 4
	 * 48 - 51: nb_channels
	 * 52 - 55: bitrate: -1
	 * 56 - 59: frame_size: 160
	 * 60 - 63: vbr
	 * 64 - 67: frames_per_packet
	 * 68 - 71: extra_headers: 0
	 * 72 - 75: reserved1
	 * 76 - 79: reserved2
	 * </pre>
	 * 
	 * @param packet
	 * @param offset
	 * @param bytes
	 * @return
	 */
	  private boolean readSpeexHeader(final byte[] packet,
	                                  final int offset,
	                                  final int bytes)
	  {
	    if (bytes!=80) {
	      System.out.println("Oooops");
	      return false;
	    }
	    if (!"Speex   ".equals(new String(packet, offset, 8))) {
	      return false;
	    }
	    mode       = packet[40+offset] & 0xFF;
	    sampleRate = readInt(packet, offset+36);
	    channels   = readInt(packet, offset+48);
	    nframes    = readInt(packet, offset+64);
	    speexDecoder = new SpeexDecoder();
	    return speexDecoder.init(mode, sampleRate, channels, enhanced);
	  }
	
	  /**
	   * Converts Little Endian (Windows) bytes to an int (Java uses Big Endian).
	   * @param data the data to read.
	   * @param offset the offset from which to start reading.
	   * @return the integer value of the reassembled bytes.
	   */
	  protected static int readInt(final byte[] data, final int offset)
	  {
	    return (data[offset] & 0xff) |
	           ((data[offset+1] & 0xff) <<  8) |
	           ((data[offset+2] & 0xff) << 16) |
	           (data[offset+3] << 24); // no 0xff on the last one to keep the sign
	    
//	    return (data[offset] << 24) |
//		           ((data[offset+1] & 0xff) <<  16) |
//		           ((data[offset+2] & 0xff) << 8) |
//		           (data[offset+3] & 0xff); // no 0xff on the last one to keep the sign
	  }	
	
	public void setPlaying(boolean isPlaying) {
		synchronized (mutex) {
			this.isPlaying = isPlaying;
			if (this.isPlaying) {
				mutex.notify();
			}
		}
	}

	public boolean isPlaying() {
		synchronized (mutex) {
			return isPlaying;
		}
	}

}
