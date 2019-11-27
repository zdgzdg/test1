/* global QUnit */
QUnit.config.autostart = false;

sap.ui.getCore().attachInit(function () {
	"use strict";

	sap.ui.require([
		"znmui5/zui5/test/unit/AllTests"
	], function () {
		QUnit.start();
	});
});