package org.apache.cordova.nonin;

import java.io.Serializable;

/**
 * Represents a frame of data received from the Nonin pulse oximeter
 * @author Oliver Gibson, Dario Salvi
 *
 */
public class NoninFrame implements Serializable{

	private static final long serialVersionUID = -8209736203001083980L;

	/**
	 * Constants for the pulse signal quality levels reported by the oximeter.
	 * Only present for 12 packets (160 milliseconds) during a pulse.
	 */
	public enum PulseSignalQuality {

		/**
		 * No indication is available (a pulse is not occurring at the time of measurement).
		 */
		OutsidePulse,

		/**
		 * Low/no pulse signal (occurs only during pulse).
		 */
		Red,

		/**
		 * Low/marginal pulse signal (occurs only during pulse).
		 */
		Yellow,

		/**
		 * High quality pulse signal (occurs only during pulse).
		 */
		Green
	}


	private static final int STATUS_BYTE = 0;
	private static final int PLETH_MSB_BYTE = 1;
	private static final int PLETH_LSB_BYTE = 2;
	private static final int EXTRA_STATUS_BYTE = 3;
	private static final int CHECKSUM_BYTE = 4;

	/**
	 * Status byte (Byte 1 of packet)
	 */
	private int status;

	/**
	 * PPG sample (Bytes 2 and 3 of packet)
	 */
	private int pleth;

	/**
	 * Byte 4 of packet, whose meaning depends on the packet number within the frame
	 */
	private int extraStatus;


	/**
	 * Populate this object with values from the buffer of raw data
	 * @param buffer Raw data received from pulse oximeter
	 */
	public NoninFrame(byte[] buffer) {

		// Treat the raw data as unsigned, not signed (which is the default for byte)
		status = buffer[STATUS_BYTE] & 0xFF;
		pleth = (buffer[PLETH_MSB_BYTE] & 0xFF) << 8;
		pleth += (buffer[PLETH_LSB_BYTE] & 0xFF);
		extraStatus = buffer[EXTRA_STATUS_BYTE] & 0xFF;
	}

	/**
	 * Checks whether this buffer contains a valid packet
	 * @param packetBuffer Raw data received from pulse oximeter
	 * @return true if the raw data represents a valid packet, false otherwise
	 */
	public static boolean IsValidFrame(byte[] packetBuffer) {

		// Treat the raw data as unsigned numbers, not signed numbers (default for byte)
		int statusByte = packetBuffer[STATUS_BYTE] & 0xFF;
		int plethMSBByte = packetBuffer[PLETH_MSB_BYTE] & 0xFF;
		int plethLSBByte = packetBuffer[PLETH_LSB_BYTE] & 0xFF;
		int extraStatusByte = packetBuffer[EXTRA_STATUS_BYTE] & 0xFF;
		int checksumByte = packetBuffer[CHECKSUM_BYTE] & 0xFF;

		// Bit 7 of Byte 1 (status) must always be set
		if ( (statusByte & 0x80) == 0 )
			return false;

		// Byte 5 (Checksum) must be the checksum of the previous four bytes
		int checksum = (statusByte + plethMSBByte + plethLSBByte + extraStatusByte) % 256;


		return (checksumByte == checksum);
	}

	/**
	 * Checks whether this packet is a "sync" packet (the first packet in a frame)
	 * @param packetBuffer Raw data received from pulse oximeter
	 * @return true if this is a "sync" packet, false otherwise
	 */
	public static boolean IsSyncFrame(byte[] packetBuffer) {
		int statusByte = packetBuffer[STATUS_BYTE] & 0xFF;

		// If this is the first packet in a frame, bit 0 of Byte 1 (status) is set
		return ( (statusByte & 0x01) == 1);
	}

	/**
	 * Returns the status information for this frame
	 * @return 8-bit number containing status flags
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Returns the additional data for this frame (whose meaning depends on the location of the frame within the packet)
	 * @return 8-bit number containing extra data included in the frame
	 */
	public int getExtraStatus() {
		return extraStatus;
	}

	/**
	 * Returns the PPG sample from this frame
	 * @return 16-bit PPG value
	 */
	public int getPleth() {
		return pleth;
	}

	/**
	 * Indicates the presence of artifact (only during the pulse).
	 * @return true during a pulse which contains artifact, false otherwise
	 */
	public boolean hasArtifact() {
		// Bit 5 of the status byte indicates artifact (ARTF)
		// 0x20 = 100000 in binary
		return ( (status & 0x20) != 0);

	}

	/**
	 * Indicates sustained artifact (absence of consecutive good pulse signals)
	 * @return true if signal is out of track, false otherwise
	 */
	public boolean isOutOfTrack() {
		// Bit 4 of the status byte indicates out-of-track (OOT)
		// 0x10 = 10000 in binary
		return ( (status & 0x10) != 0);
	}

	/**
	 * Indicates a sensor error (device is providing unusable data for analysis; set when the finger is removed)
	 * @return true during a sensor error, false otherwise
	 */
	public boolean hasSensorAlarm() {
		// Bit 3 of the status byte indicates out-of-track (SNSF)
		// 0x08 = 1000 in binary
		return ( (status & 0x08) != 0);

	}

	/**
	 * Indicates the quality of the signal.
	 * Only available for 160 milliseconds during a pulse.
	 * @return Representation of signal quality during pulse
	 */
	public PulseSignalQuality getPulseSignalQuality() {
		// Bits 1 (RPRF) and 2 (GPRF) indicate which of the four options is applicable
		boolean redBit = ((status & 0x04) > 0); // bit 2, 0x04 = 100 in binary
		boolean greenBit = ((status & 0x02) > 0); // bit 1, 0x02 = 10 in binary

		if(redBit && greenBit) // both
			return PulseSignalQuality.Yellow;
		if(redBit) // red only
			return PulseSignalQuality.Red;
		if(greenBit) // green only
			return PulseSignalQuality.Green;

		return PulseSignalQuality.OutsidePulse; // neither
	}
}
