package uk.ac.ox.ibme.nonin;

import java.io.Serializable;

/**
 * Represents a packet of data received from the Nonin pulse oximeter.
 * Packets are received every 1/3 of a second.
 */
public class NoninPacket implements Serializable {

	private static final long serialVersionUID = 4575565138976337227L;

	/**
	 * Number of frames held in each frame
	 */
	public static final int PACKETS_PER_FRAME = 25;

	/**
	 * Invalid DATA flag to be sent in case of not complete packet
	 */
	public static final int INVALID_DATA = -1;

	public static final int MISSING_HR = 511;
	public static final int MISSING_SPO2 = 127;

	
	
	/**
	 * Defines the indices of particular frames within the packet
	 */
	class FrameLocation {

		/**
		 * MSB of 4-beat average heart rate. Formatted for recording (i.e.
		 * immediately changes to an error value if finger is removed).
		 */
		public static final int HR_MSB = 0;

		/**
		 * LSB of 4-beat average heart rate. Formatted for recording (i.e.
		 * immediately changes to an error value if finger is removed).
		 */
		public static final int HR_LSB = 1;

		/**
		 * 4-beat average SpO2. Formatted for recording (i.e. immediately
		 * changes to an error value if finger is removed).
		 */
		public static final int SPO2 = 2;

		/**
		 * Oximeter firmware revision level.
		 */
		public static final int SOFTWARE_REVISION = 3;

		// Packet 4 is reserved

		/**
		 * MSB of 3 Hz timer.
		 */
		public static final int TIMER_MSB = 5;

		/**
		 * LSB of 3 Hz timer.
		 */
		public static final int TIMER_LSB = 6;

		/**
		 * STAT2 (contains low battery and SmartPoint flags).
		 */
		public static final int STAT2 = 7;

		/**
		 * 4-beat SpO2 average. Formatted for display (i.e. stays at last value
		 * for 10 seconds after finger is removed).
		 */
		public static final int SPO2_DISPLAY = 8;

		/**
		 * 4-beat SpO2 average, optimised for fast responding. Formatted for
		 * recording (i.e. changes to error value immediately if finger is
		 * removed).
		 */
		public static final int SPO2_FAST = 9;

		/**
		 * Beat-to-beat SpO2 (unaveraged). Formatted for recording (i.e. changes
		 * to error value immediately if finger is removed).
		 */
		public static final int SPO2_BEAT_TO_BEAT = 10;

		// Packet 11 is reserved

		// Packet 12 is reserved

		/**
		 * MSB of 8-beat pulse rate extended average. Formatted for recording
		 * (i.e. changes to error value immediately if finger is removed).
		 */
		public static final int EXT_HR_MSB = 13;

		/**
		 * LSB of 8-beat pulse rate extended average. Formatted for recording
		 * (i.e. changes to error value immediately if finger is removed).
		 */
		public static final int EXT_HR_LSB = 14;

		/**
		 * 8-beat SpO2 extended average. Formatted for recording (i.e. changes
		 * to error value immediately if finger is removed).
		 */
		public static final int EXT_SPO2 = 15;

		/**
		 * 8-beat SpO2 extended average. Formatted for display (i.e. stays at
		 * last value for 10 seconds after finger is removed).
		 */
		public static final int EXT_SPO2_DISPLAY = 16;

		// Packet 17 is reserved

		// Packet 18 is reserved

		/**
		 * MSB of 4-beat pulse rate average. Formatted for display (i.e. stays
		 * at last value for 10 seconds after finger is removed).
		 */
		public static final int HR_MSB_DISPLAY = 19;

		/**
		 * LSB of 4-beat pulse rate average. Formatted for display (i.e. stays
		 * at last value for 10 seconds after finger is removed).
		 */
		public static final int HR_LSB_DISPLAY = 20;

		/**
		 * MSB of 8-beat pulse rate extended average. Formatted for display
		 * (i.e. stays at last value for 10 seconds after finger is removed).
		 */
		public static final int EXT_HR_MSB_DISPLAY = 21;

		/**
		 * LSB of 8-beat pulse rate extended average. Formatted for display
		 * (i.e. stays at last value for 10 seconds after finger is removed).
		 */
		public static final int EXT_HR_LSB_DISPLAY = 22;

		// Packet 23 is reserved

		// Packet 24 is reserved

	}

	/**
	 * Array of frames in this packet
	 */
	private NoninFrame[] frames;

	/**
	 * Location in the frames array where the next new frame should be stored
	 */
	private int nextUnfilledFrame = 0;
	private int lastValidFrame = -1;
	
	private boolean hasAnyPacketsWithArtifact;
	private boolean hasAnyPacketsWithOutOfTrack;
	private boolean hasAnyPacketsWithSensorAlarm;
	
	/**
	 * Initialises the packet
	 */
	public NoninPacket() {
		frames = new NoninFrame[PACKETS_PER_FRAME];
		clear(); // reset all flags, as there are no frames in the frame yet
	}

	/**
	 * Initialises the packet
	 */
	public NoninPacket(NoninPacket frame) {
		this();
		for(int i=0; i<frame.getFramesCounter(); i++){
			addFrame(frame.getFrame(i));
		}
	}

	/**
	 * Indicates whether the packet has all the required frames
	 * @return true if the frame is complete, false if some frames are missing
	 */
	public boolean isFull() {
		return (nextUnfilledFrame == PACKETS_PER_FRAME);
	}

	/**
	 * Indicates whether any frames in this packet contain pulses with artifact.
	 * @return true if any pulse artifact was detected in this packet, false otherwise
	 */
	public boolean hasAnyArtifact() {
		return hasAnyPacketsWithArtifact;
	}
	
	/**
	 * Indicates whether any frames in this packet contain pulse with sustained artifact.
	 * @return true if the oximeter reported an absence of consecutive good pulse signals, false otherwise
	 */
	public boolean hasAnyOutOfTrack() {
		return hasAnyPacketsWithOutOfTrack;
	}
	
	/**
	 * Indicates whether any frames in this packet contain unusable data for analysis.
	 * This happens when the finger is removed.
	 * @return true if the oximeter reported a sensor alarm during this frame, false otherwise
	 */
	public boolean hasAnySensorAlarm() {
		return hasAnyPacketsWithSensorAlarm;
	}
	

	/**
	 * Empty all frames from the packet, so that it is ready to construct a new
	 * packet
	 */
	public void clear() {
		// We will start storing new frames from the start of the frame
		nextUnfilledFrame = 0;
		lastValidFrame = -1;
		
		// Reset these flags (as there are no frames in the frame yet)
		hasAnyPacketsWithArtifact = false;
		hasAnyPacketsWithOutOfTrack = false;
		hasAnyPacketsWithSensorAlarm = false;
	}

	/**
	 * Adds a new frame to this packet, from raw data.
	 * If the packet is already full, the frame will be ignored.
	 * @param frame the NoninFrame to be added
	 * @return true if the frame was successfully added, false if the packet was already full
	 */
	public boolean addFrame(NoninFrame frame) {

		// Ignore if the frame is already full
		if (isFull()) {
			return false;
		}

		// Store data from the buffer into the next available frame location in the frame
		frames[nextUnfilledFrame] = frame;
		
		// Update measurement flags for the whole frame
		if(frames[nextUnfilledFrame].hasArtifact())
			hasAnyPacketsWithArtifact = true;
		if(frames[nextUnfilledFrame].isOutOfTrack())
			hasAnyPacketsWithOutOfTrack = true;
		if(frames[nextUnfilledFrame].hasSensorAlarm())
			hasAnyPacketsWithSensorAlarm = true;
		
		nextUnfilledFrame++;
		return true;
	}

	/**
	 * Retrieves the "extra" data value in the specified frame
	 * 
	 * @param packetIndex index of frame containing the required byte
	 * @return 8-bit integer from the specified packet
	 */
	private int get8BitInteger(int packetIndex) {
		return frames[packetIndex].getExtraStatus();
	}

	/**
	 * Extracts the eight-bit heart rate by combining two bytes in the packet.
	 * This is applicable to any of the heart rate estimates (4-beat or 8-beat, recorded or displayed).
	 * The MSB of the heart rate has the following bits:
	 * 0   R   R   R   R   R   HR8 HR7
	 * The LSB of the heart rate has the following bits:
	 * 0   HR6 HR5 HR4 HR3 HR2 HR1 HR0
	 * (where R indicates a reserved bit which might be 0 or 1).
	 * We therefore need to mask out the unused/reserved bits and shift the MSB to form
	 * the byte containing HR8...HR0.
	 * 
	 * @param msbPacketIndex Index of the frame containing the MSB of the required heart rate estimate
	 * @param lsbPacketIndex Index of the frame containing the LSB of the required heart rate estimate
	 * @return Heart rate as an eight-bit integer
	 */
	private int getHeartRate(int msbPacketIndex, int lsbPacketIndex) {
		final int HR_MSB_MASK = 0x03; // = binary 00000011
		final int HR_LSB_MASK = 0x7F; // = binary 01111111
		
		int heartRate = (frames[msbPacketIndex].getExtraStatus() & HR_MSB_MASK) << 7;
		heartRate += frames[lsbPacketIndex].getExtraStatus() & HR_LSB_MASK;
		return heartRate;
	}
		
	/**
	 * Four-beat SpO2 average, formatted for recording purposes. This value is
	 * updated every 1/3 of a second. If the finger is removed from the device,
	 * the value MISSING_SPO2 is returned.
	 * 
	 * @return SpO2 average, or MISSING_SPO2 if no value could be calculated by
	 *         the Nonin device, or INVALID_DATA if the frame is not complete
	 */
	public int getSpO2Average() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.SPO2) {
			return get8BitInteger(FrameLocation.SPO2);
		} else {
			return INVALID_DATA;
		}
	}

	/**
	 * Four-beat pulse rate average, formatted for recording purposes. This
	 * value is updated every 1/3 of a second. If the finger is removed from the
	 * device, the value MISSING_HR is returned.
	 * 
	 * @return HR average, or MISSING_HR if no value could be calculated by the
	 *         Nonin device, or INVALID_DATA if the frame is not complete
	 */
	public int getHRAverage() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.HR_LSB) {
			return getHeartRate(FrameLocation.HR_MSB, FrameLocation.HR_LSB);
		} else {
			return INVALID_DATA;
		}
	}

	/**
	 * Eight-beat pulse rate average, formatted for recording purposes. This
	 * value is updated every 1/3 of a second. If the finger is removed from the
	 * device, the value MISSING_HR is returned.
	 * 
	 * @return HR average, or MISSING_HR if no value could be calculated by the
	 *         Nonin device, or INVALID_DATA if the frame is not complete
	 */
	public int getHRExtendedAverage() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.EXT_HR_LSB) {
			return getHeartRate(FrameLocation.EXT_HR_MSB, FrameLocation.EXT_HR_LSB);
		} else {
			return INVALID_DATA;
		}
	}

	/**
	 * Eight-beat SpO2 average, formatted for recording purposes. This value is
	 * updated every 1/3 of a second. If the finger is removed from the device,
	 * the value MISSING_SPO2 is returned immediately.
	 * 
	 * @return SpO2 average, or MISSING_SPO2 if no value could be calculated by
	 *         the Nonin device, or INVALID_DATA if the frame is not complete
	 */
	public int getSpO2ExtendedAverage() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.EXT_SPO2) {
			return get8BitInteger(FrameLocation.EXT_SPO2);
		} else {
			return INVALID_DATA;
		}
	}

	/**
	 * Four-beat SpO2 average, optimised for fast response This value is updated
	 * every 1/3 of a second. If the finger is removed from the device, the
	 * value MISSING_SPO2 is returned immediately.
	 * 
	 * @return SpO2 average, or MISSING_SPO2 if no value could be calculated by
	 *         the Nonin device, or INVALID_DATA if the frame is not complete
	 */
	public int getFastSpO2Average() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.SPO2_FAST) {
			return get8BitInteger(FrameLocation.SPO2_FAST);
		} else {
			return INVALID_DATA;
		}
		
	}

	/**
	 * Beat-to-beat SpO2 measurement, with no averaging This value is updated
	 * every 1/3 of a second. If the finger is removed from the device, the
	 * value MISSING_SPO2 is returned immediately.
	 * 
	 * @return SpO2 measurement, or MISSING_SPO2 if no value could be calculated
	 *         by the Nonin device, or INVALID_DATA if the frame is not complete
	 */
	public int getBeatToBeatSpO2() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.SPO2_BEAT_TO_BEAT) {
			return get8BitInteger(FrameLocation.SPO2_BEAT_TO_BEAT);
		} else {
			return INVALID_DATA;
		}
	}

	/**
	 * Displayed four-beat pulse rate average. This value is updated every 1/3
	 * of a second. If the finger is removed, this stays at the last value for
	 * 10 seconds, before indicating a missing value.
	 * 
	 * @return Four-beat pulse rate average, as displayed on the oximeter
	 *         screen, or MISSING_HR if no value could be calculated by the
	 *         Nonin device, or INVALID_DATA if the frame is not complete
	 */
	public int getDisplayedHRAverage() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.HR_LSB_DISPLAY) {
			return getHeartRate(FrameLocation.HR_MSB_DISPLAY, FrameLocation.HR_LSB_DISPLAY);
		} else {
			return INVALID_DATA;
		}
		
	}

	/**
	 * Displayed four-beat SpO2 average. This value is updated every 1/3 of a
	 * second. If the finger is removed, this stays at the last value for 10
	 * seconds, before indicating a missing value.
	 * 
	 * @return Four-beat SpO2 average, as displayed on the oximeter screen, or
	 *         MISSING_SPO2 if no value could be calculated by the Nonin device,
	 *         or INVALID_DATA if the frame is not complete
	 */
	public int getDisplayedSpO2Average() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.SPO2_DISPLAY) {
			return get8BitInteger(FrameLocation.SPO2_DISPLAY);
		} else {
			return INVALID_DATA;
		}
		
	}

	/**
	 * Displayed eight-beat SpO2 average. This value is updated every 1/3 of a
	 * second. If the finger is removed, this stays at the last value for 10
	 * seconds, before indicating a missing value.
	 * 
	 * @return Eight-beat SpO2 average, as displayed on the oximeter screen, or
	 *         MISSING_SPO2 if no value could be calculated by the Nonin device,
	 *         or INVALID_DATA if the frame is not complete
	 */
	public int getDisplayedSpO2ExtendedAverage() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.EXT_SPO2_DISPLAY) {
			return get8BitInteger(FrameLocation.EXT_SPO2_DISPLAY);
		} else {
			return INVALID_DATA;
		}
		
	}

	/**
	 * Displayed eight-beat pulse rate average. This value is updated every 1/3
	 * of a second. If the finger is removed, this stays at the last value for
	 * 10 seconds, before indicating a missing value.
	 * 
	 * @return Eight-beat pulse rate average, as displayed on the oximeter
	 *         screen, or MISSING_HR if no value could be calculated by the
	 *         Nonin device, or INVALID_DATA if the frame is not complete
	 */
	public int getDisplayedHRExtendedAverage() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.EXT_HR_LSB_DISPLAY) {
			return getHeartRate(FrameLocation.EXT_HR_MSB_DISPLAY, FrameLocation.EXT_HR_LSB_DISPLAY);
		} else {
			return INVALID_DATA;
		}
		
	}

	/**
	 * Oximeter firmware revision level
	 * 
	 * @return 8-bit number indicating the oximeter's firmware revision level,
	 * 			or INVALID_DATA if the frame is not complete
	 */
	public int getFirmwareVersion() {
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.SOFTWARE_REVISION) {
			return get8BitInteger(FrameLocation.SOFTWARE_REVISION);
		} else {
			return INVALID_DATA;
		}
		
	}
	
	
	/**
	 * Nonin 3 Hz timer value
	 * 
	 * @return 16-bit number indicating the timestamp provided by the oximeter
	 *         to this packet, or INVALID_DATA if the frame is not complete
	 */
	public int getTimer() {
		
		if ( lastValidFrame == -1 || lastValidFrame >= FrameLocation.TIMER_LSB) {
			// The timer is a 14-bit number.
			// It is transmitted as the least significant 7 bits of the MSB and LSB bytes
			final int TIMER_MASK = 0x7F; // = binary 01111111
			
			int timerValue = (frames[FrameLocation.TIMER_MSB].getExtraStatus() & TIMER_MASK) << 7;
			timerValue += (frames[FrameLocation.TIMER_LSB].getExtraStatus() & TIMER_MASK);
			return timerValue;
		} else {
			return INVALID_DATA;
		}
		
	}

	/**
	 * Indicates whether this is a high-quality SmartPoint measurement
	 * 
	 * @return true if this is a SmartPoint measurement, false otherwise
	 */
	public boolean isSmartPointMeasurement() {
		int statusByte2 = get8BitInteger(FrameLocation.STAT2);

		// SPA (SmartPoint measurement) is indicated by bit 5 of the STAT2 byte
		// being set
		// 0x20 = 00100000 binary
		return ((statusByte2 & 0x20) != 0);

	}

	/**
	 * Indicates whether the oximeter's battery is low
	 * 
	 * @return true if battery is low (replace as soon as possible), false
	 *         otherwise
	 */
	public boolean isBatteryLow() {
		int statusByte2 = get8BitInteger(FrameLocation.STAT2);

		// Low battery is indicated by bit 0 of the STAT2 byte being set
		return ((statusByte2 & 0x01) != 0);
	}

	/**
	 * Gets the sampled PPG waveform.	
	 * @return array of PPG samples, one from each packet in the frame
	 */
	public int[] getPlethSamples() {
		
		int[] plethSamples = new int[PACKETS_PER_FRAME];
		for(int packetNum = 0; packetNum < PACKETS_PER_FRAME; packetNum++)
			plethSamples[packetNum] = frames[packetNum].getPleth();
		return plethSamples;
		
	}
	
	/**
	 * Gets the frame at the specified index (from 0 to 24)
	 * @param packetIndex, from 0 to 24
	 * @return frame at the index
	 */
	public NoninFrame getFrame(int packetIndex) {
		return frames[packetIndex];
	}
	
	
	
	/**
	 * Stores the last nextUnfilledFrame as the last
	 */
	private void setLastValidFrame() {
		if (lastValidFrame == -1) {
			lastValidFrame = nextUnfilledFrame;
		}
	}

	/**
	 * Gets the current frames counter (from 0 to 25)
	 * @return the number of frames currently inside the packet
	 */
	public int getFramesCounter() {
		return nextUnfilledFrame;
	}
	
	/**
	 * Stores the last nextUnfilledFrame as the last
	 * Should return -1 in case of full and valid frame
	 */
	private int getLastValidFrame() {
		return lastValidFrame;
	}
	
}

