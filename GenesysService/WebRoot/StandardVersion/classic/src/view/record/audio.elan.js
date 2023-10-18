/*!
 * wavesurfer.js elan plugin 3.0.0 (2019-08-04)
 * https://github.com/katspaugh/wavesurfer.js
 * @license BSD-3-Clause
 */
(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define("elan", [], factory);
	else if(typeof exports === 'object')
		exports["elan"] = factory();
	else
		root["WaveSurfer"] = root["WaveSurfer"] || {}, root["WaveSurfer"]["elan"] = factory();
})(window, function() {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, { enumerable: true, get: getter });
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 			Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 		}
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// create a fake namespace object
/******/ 	// mode & 1: value is a module id, require it
/******/ 	// mode & 2: merge all properties of value into the ns
/******/ 	// mode & 4: return value when already ns object
/******/ 	// mode & 8|1: behave like require
/******/ 	__webpack_require__.t = function(value, mode) {
/******/ 		if(mode & 1) value = __webpack_require__(value);
/******/ 		if(mode & 8) return value;
/******/ 		if((mode & 4) && typeof value === 'object' && value && value.__esModule) return value;
/******/ 		var ns = Object.create(null);
/******/ 		__webpack_require__.r(ns);
/******/ 		Object.defineProperty(ns, 'default', { enumerable: true, value: value });
/******/ 		if(mode & 2 && typeof value != 'string') for(var key in value) __webpack_require__.d(ns, key, function(key) { return value[key]; }.bind(null, key));
/******/ 		return ns;
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "localhost:8080/dist/plugin/";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = "./src/plugin/elan.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./src/plugin/elan.js":
/*!****************************!*\
  !*** ./src/plugin/elan.js ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

/**
 * @typedef {Object} ElanPluginParams
 * @property {string|HTMLElement} container CSS selector or HTML element where
 * the ELAN information should be rendered.
 * @property {string} url The location of ELAN XML data
 * @property {?boolean} deferInit Set to true to manually call
 * @property {?Object} tiers If set only shows the data tiers with the `TIER_ID`
 * in this map.
 */

/**
 * Downloads and renders ELAN audio transcription documents alongside the
 * waveform.
 *
 * @implements {PluginClass}
 * @extends {Observer}
 * @example
 * // es6
 * import ElanPlugin from 'wavesurfer.elan.js';
 *
 * // commonjs
 * var ElanPlugin = require('wavesurfer.elan.js');
 *
 * // if you are using <script> tags
 * var ElanPlugin = window.WaveSurfer.elan;
 *
 * // ... initialising wavesurfer with the plugin
 * var wavesurfer = WaveSurfer.create({
 *   // wavesurfer options ...
 *   plugins: [
 *     ElanPlugin.create({
 *       // plugin options ...
 *     })
 *   ]
 * });
 */
var ElanPlugin =
/*#__PURE__*/
function () {
  _createClass(ElanPlugin, null, [{
    key: "create",

    /**
     * Elan plugin definition factory
     *
     * This function must be used to create a plugin definition which can be
     * used by wavesurfer to correctly instantiate the plugin.
     *
     * @param  {ElanPluginParams} params parameters use to initialise the plugin
     * @return {PluginDefinition} an object representing the plugin
     */
    value: function create(params) {
      return {
        name: 'elan',
        deferInit: params && params.deferInit ? params.deferInit : false,
        params: params,
        instance: ElanPlugin
      };
    }
  }]);

  function ElanPlugin(params, ws) {
    _classCallCheck(this, ElanPlugin);

    this.Types = {
      ALIGNABLE_ANNOTATION: 'ALIGNABLE_ANNOTATION',
      REF_ANNOTATION: 'REF_ANNOTATION'
    };
    this.data = null;
    this.annotations = {};
    this.params = params;
    this.container = 'string' == typeof params.container ? document.querySelector(params.container) : params.container;

    if (!this.container) {
      throw Error('No container for ELAN');
    }
  }

  _createClass(ElanPlugin, [{
    key: "init",
    value: function init() {
      this.bindClick();

      if (this.params.url) {
        this.load(this.params.url);
      }
    }
  }, {
    key: "destroy",
    value: function destroy() {
      this.container.removeEventListener('click', this._onClick);
      this.container.removeChild(this.table);
    }
  }, {
    key: "load",
    value: function load(json) {
      var _this = this;

      // table
      var table = this.table = document.createElement('table');
      table.className = 'audio-tab-table'; // head

      var thead = document.createElement('thead');
      var headRow = document.createElement('tr');
      thead.appendChild(headRow);
      table.appendChild(thead);
      var thtime = document.createElement('th');
      thtime.textContent = '时间';
      thtime.className = '';
      thtime.width = '70px';
      headRow.appendChild(thtime);
      var thtext = document.createElement('th');
      thtext.className = '';
      thtext.textContent = '内容';
      headRow.appendChild(thtext); // body

      var tbody = document.createElement('tbody');
      table.appendChild(tbody);

      if (json.length == 0) {
        var row = document.createElement('tr');
        row.id = 'tab-audio-00000000-0000-0000-0000-000000000000'; //唯一编号

        var td = document.createElement('td');
        td.colSpan = 2;
        td.align = 'center';
        td.style.fontSize = '18pt';
        td.style.height = '193px';
        td.style.color = '#949495';
        td.textContent = '没有文字内容(需要语音识别模块)';
        row.appendChild(td);
        tbody.appendChild(row);
      } else {
        json.forEach(function (item) {
          item.id.start /= 1000;
          item.id.end /= 1000;
          var row = document.createElement('tr');
          row.id = 'tab-audio-' + item.id.id + item.id.start; //唯一编号

          tbody.appendChild(row);
          var tdtime = document.createElement('td');
          tdtime.align = 'center';
          tdtime.textContent = Ext.Date.format(new Date(item.starttime), 'H:i:s');
          row.appendChild(tdtime);
          var tdtext = document.createElement('td');
          tdtext.dataset.ref = item.id.id + item.id.start;
          tdtext.dataset.start = item.id.start;
          tdtext.dataset.end = item.id.end;
          tdtext.textContent = item.audiocontent;
          row.appendChild(tdtext);
          _this.annotations[item.id.id + item.id.start] = item.id;
        });
      }

      this.data = json;
      this.container.innerHTML = '';
      this.container.appendChild(table);
      this.fireEvent('ready', json);
    }
  }, {
    key: "bindClick",
    value: function bindClick() {
      var _this2 = this;

      this._onClick = function (e) {
        var ref = e.target.dataset.ref;

        if (null != ref) {
          var annot = _this2.annotations[ref];

          if (annot) {
            _this2.fireEvent('select', annot.start, annot.end);
          }
        }
      };

      this.container.addEventListener('click', this._onClick);
    }
  }, {
    key: "getRenderedAnnotation",
    value: function getRenderedAnnotation(time) {
      var result;
      this.data.some(function (item) {
        if (item.id.start <= time && item.id.end >= time) {
          result = item.id;
          return true;
        }

        return false;
      });
      return result;
    }
  }, {
    key: "getAnnotationNode",
    value: function getAnnotationNode(annotation) {
      return document.getElementById('tab-audio-' + annotation.id + annotation.start);
    }
  }]);

  return ElanPlugin;
}();

exports.default = ElanPlugin;
module.exports = exports.default;

/***/ })

/******/ });
});
//# sourceMappingURL=wavesurfer.elan.js.map