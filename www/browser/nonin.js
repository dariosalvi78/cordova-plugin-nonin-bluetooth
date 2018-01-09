
var exec = require("cordova/exec");

var Nonin = function () {
    this.name = "Nonin";
	this.timer;
};

Nonin.prototype.isBTON = function (onSuccess, onError) {
    onSuccess(true);
};

Nonin.prototype.askBTON = function (onSuccess, onError) {
    onSuccess(true);
};

Nonin.prototype.isPaired = function (address, onSuccess, onError) {
    onSuccess(true);
};

Nonin.prototype.start = function (address, onSuccess, onError) {
	clearTimeout(this.timer);
	this.timer = setInterval(function() {
		onSuccess({
			timestamp: new Date().getTime(),
			spo2: Math.floor(95 + Math.random()*5),
			hr: Math.floor(70 + Math.random()*5),
			hasArtifacts: false,
			hasSustainedArtifacts: false,
			nofinger: false,
			batterylow: false
		})
	}, 500);
};

Nonin.prototype.stop = function (onSuccess, onError) {
	clearTimeout(this.timer);
	onSuccess(true);
};

module.exports = new Nonin();
