
	
	var exec = require('cordova/exec');
	var BPushPlugin = function(){
	};

	BPushPlugin.prototype.error_callback = function(msg) {
		console.log("Javascript Callback Error: " + msg);
	}

	BPushPlugin.prototype.call_native = function(name, args, callback) {
		ret = cordova.exec(callback, this.error_callback, 'BPushPlugin', name, args);
		return ret;
	}

	BPushPlugin.prototype.start = function(userId, deviceId, port, host, callback) {
		this.call_native("bind", [userId, deviceId, port, host], callback);
	}

	BPushPlugin.prototype.stop = function(args, callback) {
		args = args || [];
		this.call_native("unbind", [], callback);
	}

	window.plugins = window.plugins || {};
	window.plugins.BPushPlugin = window.plugins.BPushPlugin || new BPushPlugin();

	module.exports = new BPushPlugin();