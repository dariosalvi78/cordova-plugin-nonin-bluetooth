
var exec = require("cordova/exec");

var Nonin = function () {
    this.name = "Nonin";
};

Nonin.prototype.askPermissions = function (onSuccess, onError) {
    exec(onSuccess, onError, "Nonin", "askPermissions", []);
};

Nonin.prototype.isBTON = function (onSuccess, onError) {
    exec(onSuccess, onError, "Nonin", "isBTON", []);
};

Nonin.prototype.askBTON = function (onSuccess, onError) {
    exec(onSuccess, onError, "Nonin", "askBTON", []);
};

Nonin.prototype.isPaired = function (address, onSuccess, onError) {
    exec(onSuccess, onError, "Nonin", "isPaired", [address]);
};

Nonin.prototype.start = function (address, onSuccess, onError) {
    exec(onSuccess, onError, "Nonin", "start", [address]);
};

Nonin.prototype.stop = function (onSuccess, onError) {
    exec(onSuccess, onError, "Nonin", "stop", []);
};

module.exports = new Nonin();
