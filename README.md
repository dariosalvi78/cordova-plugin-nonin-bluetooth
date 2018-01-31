## Cordova Plugin for Nonin Bluetooth (not BLE)  pulse oximeters

**Only works with Android**

## Platform and device support

Android only. Compatible with Nonin BlueTooth models: Onyx II (Model 9560) and WristOx2 (Model 3150).

It works over Bluetooth Serial Port Profile (SPP).
It only implements "Serial Data Format #7" mode.
This mode provides continuous data transmission of a data packet sent 75 times per second.
The data packet includes real-time data including: 8-bit waveform value, six different output options for the SpO2 value,
four different averaging options for the pulse rate values, and options formatted for both
recording and display purposes, as well as status information for the measurement and status of the battery.

Protocol details are [on the manufacturers web site](http://www.nonin.com/documents/6470_ENG.pdf).

## Install

```
cordova plugin add cordova-plugin-nonin-bluetooth
```

## Programming interface

Before using it, the user must have paired the device with Android and must know the BlueTooth address
You **do not** need to reference any JavaScript, the Cordova plugin architecture will add a nonin object to your root automatically when you build.

### askPermissions

Asks for needed permissions

```js
nonin.askPermissions(successCallback, failureCallback);
```
- => `successCallback` is called when permissions are granted
- => `failureCallback` is called if there was an error or permissions were not granted


### isBTON

Tells if BlueTooth is ON

```js
nonin.isBTON(successCallback, failureCallback);
```
- => `successCallback` is called with argument true if BlueTooth is ON, otherwise false
- => `failureCallback` is called if there was an error

### askBTON

Asks the user to switch BlueTooth ON

```js
nonin.askBTON(successCallback, failureCallback);
```
- => `successCallback` is called if the BlueTooth was switched on
- => `failureCallback` is called if there was an error

If the user denies switching on the BlueTooth, no successCallback is called. You should verify if BT was enabled after a certain time has passed.

### isPaired

Tells if a given address is in the list of paired devices.
The BlueTooth interface must be on to work.

```js
nonin.isPaired(address, successCallback, failureCallback);
```
- => `address` is the BlueTooth address of the device like xx:xx:xx:xx:xx:xx
- => `successCallback` is called with true if the device was paired, otherwise false
- => `failureCallback` is called if there was an error

### start

Starts getting data from the device. A callback is called each time a new packet is retrieved from the device (about 3 per second).
The BlueTooth adapter must be on to work.

```js
var successHandler = function (data) {
     // data.spo2 -> blood saturation (avg over 4 pulses)
     // data.instantSpo2 -> instantaneous (non averaged) spo2
     // data.hr -> heart rate (avg over 4 pulses)
     // data.timestamp -> ms since 1970
     // data.timer -> internal device timer
     // data.hasArtifacts -> true if the signal has artifacts (low quality)
     // data.hasSustainedArtifacts -> true if the signal has sustained artifacts (even lower quality)
     // data.nofinger -> true if the finger was removed from the device
     // data.batterylow -> true if batteries are low
     // data.sensorAlarm -> true if data is unusable
     // data.smartPoint -> true if very precise measurement
     // data.PPG -> array of PPG samples
};
nonin.start(address, successCallback, failureCallback);
```

- => `address` is the BlueTooth address of the device like xx:xx:xx:xx:xx:xx
- => `successCallback` is called each time a packet is received, data is passed as argument
- => `failureCallback` is called if there was an error (eg the device was not paired or BlueTooth was off)


### stop

Disconnects from the sensor.

```js
nonin.stop(successCallback, failureCallback);
```
- => `successCallback` is called if stopped
- => `failureCallback` is called if there was an error
