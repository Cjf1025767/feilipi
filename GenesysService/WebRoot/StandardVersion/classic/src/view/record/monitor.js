'use strict';function _typeof(a){"@babel/helpers - typeof";return _typeof="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(a){return typeof a}:function(a){return a&&"function"==typeof Symbol&&a.constructor===Symbol&&a!==Symbol.prototype?"symbol":typeof a},_typeof(a)}/**
 * JsonRpcClient
 *
 * A JSON RPC Client that uses WebSockets if available otherwise fallbacks to ajax.
 * Depends on JSON, if browser lacks native support either use JSON3 or jquery.json.
 * Usage example:
 *
 *   var foo = new $.JsonRpcClient({ ajaxUrl: '/backend/jsonrpc' });
 *   foo.call(
 *     'bar', [ 'A parameter', 'B parameter' ],
 *     function(result) { alert('Foo bar answered: ' + result.my_answer); },
 *     function(error)  { console.log('There was an error', error); }
 *   );
 *
 * More examples are available in README.md
 */(function(a){"function"==typeof define&&define.amd?define(["jquery"],a):"object"===("undefined"==typeof exports?"undefined":_typeof(exports))?a(require("jquery")):a(jQuery)})(function(a){/**
   * @fn new
   * @memberof JsonRpcClient
   *
   * @param {object} options An object stating the backends:
   *                ajaxUrl    A url (relative or absolute) to a http(s) backend.
   *                headers    An object that will be passed along to $.ajax in options.headers
   *                xhrFields  An object that will be passed along to $.ajax in options.xhrFields
   *                socketUrl  A url (relative of absolute) to a ws(s) backend.
   *                onmessage  A socket message handler for other messages (non-responses).
   *                onopen     A socket onopen handler. (Not used for custom getSocket.)
   *                onclose    A socket onclose handler. (Not used for custom getSocket.)
   *                onerror    A socket onerror handler. (Not used for custom getSocket.)
   *                getSocket  A function returning a WebSocket or null.
   *                           It must take an onmessage_cb and bind it to the onmessage event
   *                           (or chain it before/after some other onmessage handler).
   *                           Or, it could return null if no socket is available.
   *                           The returned instance must have readyState <= 1, and if less than 1,
   *                           react to onopen binding.
   *                timeout    (optional) A number of ms to wait before timing out and failing a
   *                           call. If specified a setTimeout will be used to keep track of calls
   *                           made through a websocket.
   */var b=function(b){var c=this,d=function(){};// Declare an instance version of the onmessage callback to wrap 'this'.
/// Holding the WebSocket on default getsocket.
/// Object <id>: { success_cb: cb, error_cb: cb }
/// The next JSON-RPC request id.
//queue for ws request sent *before* ws is open.
this.options=a.extend({ajaxUrl:null,headers:{},///< Optional additional headers to send in $.ajax request.
socketUrl:null,///< WebSocket URL. (Not used if a custom getSocket is supplied.)
onmessage:d,///< Optional onmessage-handler for WebSocket.
onopen:d,///< Optional onopen-handler for WebSocket.
onclose:d,///< Optional onclose-handler for WebSocket.
onerror:d,///< Optional onerror-handler for WebSocket.
/// Custom socket supplier for using an already existing socket
getSocket:function(a){return c._getSocket(a)}},b),this.wsOnMessage=function(a){c._wsOnMessage(a)},this._wsSocket=null,this._wsCallbacks={},this._currentId=1,this._wsRequestQueue=[],this.JSON=!window.JSON&&a&&a.toJSON?{stringify:a.toJSON,parse:a.parseJSON}:JSON};/**
   * @fn call
   * @memberof JsonRpcClient
   *
   * @param {string}       method     The method to run on JSON-RPC server.
   * @param {object|array} params     The params; an array or object.
   * @param {function}     successCb  A callback for successful request.
   * @param {function}     errorCb    A callback for error.
   *
   * @return {object} Returns the deferred object that $.ajax returns or {null} for websockets
   */ /**
   * Notify sends a command to the server that won't need a response.  In http, there is probably
   * an empty response - that will be dropped, but in ws there should be no response at all.
   *
   * This is very similar to call, but has no id and no handling of callbacks.
   *
   * @fn notify
   * @memberof JsonRpcClient
   *
   * @param {string} method       The method to run on JSON-RPC server.
   * @param {object|array} params The params; an array or object.
   *
   * @return {object} Returns the deferred object that $.ajax returns or {null} for websockets
   */ /**
   * Make a batch-call by using a callback.
   *
   * The callback will get an object "batch" as only argument.  On batch, you can call the methods
   * "call" and "notify" just as if it was a normal JsonRpcClient object, and all calls will be
   * sent as a batch call then the callback is done.
   *
   * @fn batch
   * @memberof JsonRpcClient
   *
   * @param {function} callback   This function will get a batch handler to run call and notify on.
   * @param {function} allDoneCb  A callback function to call after all results have been handled.
   * @param {function} errorCb    A callback function to call if there is an error from the server.
   *                    Note, that batch calls should always get an overall success, and the
   *                    only error
   */ /**
   * The default getSocket handler.
   *
   * @param {function} onmessageCb The callback to be bound to onmessage events on the socket.
   *
   * @fn _getSocket
   * @memberof JsonRpcClient
   */ /**
   * Internal handler to dispatch a JRON-RPC request through a websocket.
   *
   * @fn _wsCall
   * @memberof JsonRpcClient
   */ /**
   * Internal handler for the websocket messages.  It determines if the message is a JSON-RPC
   * response, and if so, tries to couple it with a given callback.  Otherwise, it falls back to
   * given external onmessage-handler, if any.
   *
   * @param {event} event The websocket onmessage-event.
   */ /**
   * Internal WebSocket error handler.
   * Will execute all unresolved calls immideatly.
   **/ /**
   * Internal WebSocket close handler.
   * Will execute all unresolved calls immideatly.
   **/ /**
   * Execute error handler on all pending calls.
   */ /**
   * Create a timeout for this request
   */ /************************************************************************************************
   * Batch object with methods
   ************************************************************************************************/ /**
   * Handling object for batch calls.
   */ /**
   * @sa JsonRpcClient.prototype.call
   */ /**
   * @sa JsonRpcClient.prototype.notify
   */ /**
   * Executes the batched up calls.
   *
   * @return {object} Returns the deferred object that $.ajax returns or {null} for websockets
   */ /**
   * Internal helper to match the result array from a batch call to their respective callbacks.
   *
   * @fn _batchCb
   * @memberof JsonRpcClient
   */b.prototype.call=function(b,c,d,e){d="function"==typeof d?d:function(){},e="function"==typeof e?e:function(){};// Construct the JSON-RPC 2.0 request.
var f={jsonrpc:"2.0",method:b,params:c,id:this._currentId++// Increase the id counter to match request/response
},g=this.options.getSocket(this.wsOnMessage);// Try making a WebSocket call.
if(null!==g)return this._wsCall(g,f,d,e),null;// No WebSocket, and no HTTP backend?  This won't work.
if(null===this.options.ajaxUrl)throw"JsonRpcClient.call used with no websocket and no http endpoint.";var h=this,i=a.ajax({type:"POST",url:this.options.ajaxUrl,contentType:"application/json",data:this.JSON.stringify(f),dataType:"json",cache:!1,headers:this.options.headers,xhrFields:this.options.xhrFields,timeout:this.options.timeout,success:function(a){"error"in a?e(a.error):d(a.result)},// JSON-RPC Server could return non-200 on error
error:function(a){try{var b=h.JSON.parse(a.responseText);"console"in window&&console.log(b),e(b.error)}catch(b){// Perhaps the responseText wasn't really a jsonrpc-error.
e({error:a.responseText})}}});return i},b.prototype.notify=function(b,c){// Construct the JSON-RPC 2.0 request.
var d={jsonrpc:"2.0",method:b,params:c},e=this.options.getSocket(this.wsOnMessage);// Try making a WebSocket call.
if(null!==e)return this._wsCall(e,d),null;// No WebSocket, and no HTTP backend?  This won't work.
if(null===this.options.ajaxUrl)throw"JsonRpcClient.notify used with no websocket and no http endpoint.";var f=a.ajax({type:"POST",url:this.options.ajaxUrl,contentType:"application/json",data:this.JSON.stringify(d),dataType:"json",cache:!1,headers:this.options.headers,xhrFields:this.options.xhrFields});return f},b.prototype.batch=function(a,c,d){var e=new b._batchObject(this,c,d);a(e),e._execute()},b.prototype._getSocket=function(a){// If there is no ws url set, we don't have a socket.
// Likewise, if there is no window.WebSocket.
if(null===this.options.socketUrl||!("WebSocket"in window))return null;if(null===this._wsSocket||1<this._wsSocket.readyState){try{this._wsSocket=new WebSocket(this.options.socketUrl)}catch(a){// This can happen if the server is down, or malconfigured.
return null}// Set up onmessage handler.
this._wsSocket.onmessage=a;var b=this;// Set up onclose handler.
this._wsSocket.onclose=function(a){b._wsOnClose(a)},this._wsSocket.onerror=function(a){b._wsOnError(a)}}return this._wsSocket},b.prototype._wsCall=function(a,b,c,d){var e=this.JSON.stringify(b);// Setup callbacks.  If there is an id, this is a call and not a notify.
if("id"in b&&"undefined"!=typeof c&&(this._wsCallbacks[b.id]={successCb:c,errorCb:d}),!(1>a.readyState))this.options.timeout&&this._wsCallbacks[b.id]&&(this._wsCallbacks[b.id].timeout=this._createTimeout(b.id)),a.send(e);else if(this._wsRequestQueue.push(e),!a.onopen){// The websocket is not open yet; we have to set sending of the message in onopen.
var f=this;// In closure below, this is set to the WebSocket.  Use self instead.
// Set up sending of message for when the socket is open.
a.onopen=function(b){f.options.onopen(b);for(var c,d=f.options.timeout,e=0;e<f._wsRequestQueue.length;e++)c=f._wsRequestQueue[e],d&&f._wsCallbacks[c.id]&&(f._wsCallbacks[c.id].timeout=f._createTimeout(c.id)),a.send(c);f._wsRequestQueue=[]}}},b.prototype._wsOnMessage=function(a){// Check if this could be a JSON RPC message.
var b;try{b=this.JSON.parse(a.data)}catch(b){return void this.options.onmessage(a)}/// @todo Make using the jsonrcp 2.0 check optional, to use this on JSON-RPC 1 backends.
if("object"===_typeof(b)&&"2.0"===b.jsonrpc){/// @todo Handle bad response (without id).
// If this is an object with result, it is a response.
if("result"in b&&this._wsCallbacks[b.id]){// Get the success callback.
var c=this._wsCallbacks[b.id].successCb;// Clear any timeout
return this._wsCallbacks[b.id].timeout&&clearTimeout(this._wsCallbacks[b.id].timeout),delete this._wsCallbacks[b.id],void c(b.result)}// If this is an object with error, it is an error response.
if("error"in b&&this._wsCallbacks[b.id]){// Get the error callback.
var d=this._wsCallbacks[b.id].errorCb;// Delete the callback from the storage.
return delete this._wsCallbacks[b.id],void d(b.error)}}// If we get here it's an invalid JSON-RPC response, pass to fallback message handler.
this.options.onmessage(a)},b.prototype._wsOnError=function(a){this._failAllCalls("Socket errored."),this.options.onerror(a)},b.prototype._wsOnClose=function(a){this._failAllCalls("Socket closed."),this.options.onclose(a)},b.prototype._failAllCalls=function(a){for(var b in this._wsCallbacks)if(this._wsCallbacks.hasOwnProperty(b)){// Get the error callback.
var c=this._wsCallbacks[b].errorCb;// Run callback with the error object as parameter.
c(a)}// Throw 'em away
this._wsCallbacks={}},b.prototype._createTimeout=function(a){if(this.options.timeout){var b=this;return setTimeout(function(){if(b._wsCallbacks[a]){var c=b._wsCallbacks[a].errorCb;delete b._wsCallbacks[a],c("Call timed out.")}},this.options.timeout)}},b._batchObject=function(a,b,c){// Array of objects to hold the call and notify requests.  Each objects will have the request
// object, and unless it is a notify, successCb and errorCb.
this._requests=[],this.jsonrpcclient=a,this.allDoneCb=b,this.errorCb="function"==typeof c?c:function(){}},b._batchObject.prototype.call=function(a,b,c,d){this._requests.push({request:{jsonrpc:"2.0",method:a,params:b,id:this.jsonrpcclient._currentId++// Use the client's id series.
},successCb:c,errorCb:d})},b._batchObject.prototype.notify=function(a,b){this._requests.push({request:{jsonrpc:"2.0",method:a,params:b}})},b._batchObject.prototype._execute=function(){var b=this,c=null;// Used to store and return the deffered that $.ajax returns
if(0!==this._requests.length){// All done :P
// Collect all request data and sort handlers by request id.
var d=[],e=b.jsonrpcclient.options.getSocket(b.jsonrpcclient.wsOnMessage);// If we have a WebSocket, just send the requests individually like normal calls.
if(null!==e){// We need to keep track of results for the all done callback
for(var f,g=0,h=[],j=function(a){return b.allDoneCb?function(c){if(a(c),h.push(c),g--,0>=g){// Change order so that it maps to request order
var d,e={};for(d=0;d<h.length;d++)e[h[d].id]=h[d];var f=[];for(d=0;d<b._requests.length;d++)e[b._requests[d].id]&&f.push(e[b._requests[d].id]);// Call all done!
b.allDoneCb(f)}}:a},k=0;k<this._requests.length;k++)f=this._requests[k],"id"in f.request&&g++,b.jsonrpcclient._wsCall(e,f.request,j(f.successCb),j(f.errorCb));return null}// No websocket, let's use ajax
for(var f,l={},k=0;k<this._requests.length;k++)f=this._requests[k],d.push(f.request),"id"in f.request&&(l[f.request.id]={successCb:f.successCb,errorCb:f.errorCb});var m=function(a){b._batchCb(a,l,b.allDoneCb)};// No WebSocket, and no HTTP backend?  This won't work.
if(null===b.jsonrpcclient.options.ajaxUrl)throw"JsonRpcClient.batch used with no websocket and no http endpoint.";// Send request
return c=a.ajax({url:b.jsonrpcclient.options.ajaxUrl,contentType:"application/json",data:this.jsonrpcclient.JSON.stringify(d),dataType:"json",cache:!1,type:"POST",headers:b.jsonrpcclient.options.headers,xhrFields:b.jsonrpcclient.options.xhrFields,// Batch-requests should always return 200
error:function(a,c,d){b.errorCb(a,c,d)},success:m}),c}},b._batchObject.prototype._batchCb=function(a,b,c){for(var d,e=0;e<a.length;e++)d=a[e],"error"in d?null!==d.id&&d.id in b?b[d.id].errorCb(d.error):"console"in window&&console.log(d):!(d.id in b)&&"console"in window?console.log(d):b[d.id].successCb(d.result);"function"==typeof c&&c(a)},a.JsonRpcClient=b}),function(a){"function"==typeof define&&define.amd?define(["jquery"],a):"object"===("undefined"==typeof exports?"undefined":_typeof(exports))?a(require("jquery")):a(jQuery)}(function(a){a.Monitor=function(){function b(){return null==g&&(g=new a.JsonRpcClient({socketUrl:i+"/chat/?crmkey="+h,onmessage:c,onclose:function(){g=null}})),g}function c(a){j(a.data)}function d(a,c){i=a,j="function"==typeof c?c:function(){},null!=b()&&g.call("Monitor",{})}function e(a,c){null!=b()&&g.call("Listen",{exten:a,agent:c})}function f(c,d,e){var f=a.extend({extension:c,agent:d},{});null!=b()&&g.call("GetextensionState",f,e,function(){g=null})}var g=null,h="jdflsdjfgsdjklajdfgoej",i="ws://127.0.0.1:55588",j=function(){};return{monitor:d,listen:e,getextensionstatus:f}}});
//# sourceMappingURL=monitor.js.map
