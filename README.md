## Cordova Plugin for Nonin pulse oximeters

**Only works in Android for the moment**

## Install

```
cordova plugin add https://ibme-gitcvs.eng.ox.ac.uk/mhealth/cordova-plugin-nonin.git
```

## Programming interface

Before using it, the user must have paired the device with Android and must know the BlueTooth address
You **do not** need to reference any JavaScript, the Cordova plugin architecture will add a nonin object to your root automatically when you build.

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
The BlueTooth adapter must be on to work.

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
    // data.spo2; -> blood saturation
    // data.hr; -> heart rate
    // data.timestamp; -> ms since 1970
    // data.hasArtifacts; -> true if the signal has artifacts (low quality)
    // data.hasSustainedArtifacts; -> true if the signal has sustained artifacts (even lower quality)
    // data.nofinger; -> true if the finger was removed from the device
    // data.batterylow; -> true if batteries are low
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

## Platform and device support

Android only. Compatible with Nonin BlueTooth models: Onyx II, Wrist OX2.
