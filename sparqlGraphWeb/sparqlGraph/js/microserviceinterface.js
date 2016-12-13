/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */


define([	'sparqlgraph/js/msiresultset',
        	'sparqlgraph/js/modaliidx',
        	'jquery',
			// shimmed
			
		],

	function(MsiResultSet, ModalIidx, $) {
	
		/*
		 *    A column name or text or some item used to build a triple value
		 */
		var MicroServiceInterface = function (url) {
			// url may be the entire thing, including the endpoint
			// or it may end in '/' so that postToEndpoint() and getEndpoint() will append
			this.url = url;
			if (url.slice(-1) !== "/") {
				this.url += "/";
			}
			this.lastUrl = "";
			this.timeout = 45000;
			this.userFailureCallback = function (msg, optAllowHtml) {
				                            // optAllowHtml:  default is HTML_SAFE, which means show user the raw tags.
				                            //                If you know you're sending HTML, set this to true.
											var htmlFlag = (typeof optAllowHtml === "undefined" || !optAllowHtml) ? ModalIidx.HTML_SAFE : ModalIidx.HTML_ALLOW;
											ModalIidx.alert("Microservice Failure", msg, ModalIidx.HTML_SAFE);
											if (typeof kdlLogEvent != "undefined") {
												kdlLogEvent("SG Microservice Failure", "message", msg);
											}
										};
			this.userSuccessCallback = null;
		};
				
		MicroServiceInterface.FORM_CONTENT = false;  // 3rd param to postToEndpoint
		
		MicroServiceInterface.prototype = {
				getServiceName : function () {
					return this.url.slice(0,-1).split('/').pop();
				},
				
				errorCallback : function (xhr, status, err) { 
					this.userFailureCallback(this.generatePostFailureHtml(xhr, status, err), true);
				},
				
				statusCodeCallback : function (statusCode, object) {
					// never called because ajax stinks
					var html =  "<h3>Call to" + this.getServiceName() + " failed</h3>";
					html += "<br><b>HTTP status: &nbsp</b>" + statusCode;
					
					if (statusCode == 403) {
						html += "<br><b>meaning: &nbsp</b>" + status;
					}
					this.userFailureCallback(html, true);
				},
				
				successCallback : function (xhr, status, err) { 
					// call to the service succeeded.   
					// The service itself might have succeeded or failed.
					var resultSet = new MsiResultSet(this.lastUrl, xhr);
				
					this.userSuccessCallback(resultSet);
					
				},
				
				generatePostFailureHtml : function (xhr, status, err) { 
					// make a message out of failure callback parameters
					var ret =  "<h3>" + this.getServiceName() + " service failed</h3>";
					ret += "<br><b>url: &nbsp</b>" + this.lastUrl;
					ret += "<br><b>status: &nbsp</b>" + status;
					ret += "<br><b>error: &nbsp</b>" + err;
					ret += this.generateXhrHtml(xhr);
					
					return ret;
				},
				
				
				generateXhrHtml : function (xhr) {
					// pull anything we can find out of xhr and make it html
					ret = "";
					for (var key in xhr) {
						if (xhr.hasOwnProperty(key)) {
							var s = JSON.stringify(xhr[key]);
							// skip weird entries and functions
							if (typeof s !== "undefined" && s.slice(0,8) !== "function") {
								if (key == "responseJSON") {
									// repeat loop for response JSON
									for (var k2 in xhr.responseJSON) {
										var s2 = JSON.stringify(xhr.responseJSON[k2]);
										ret += "<br><b>" + "response." + k2 + ": &nbsp</b>" + s2.replace(/[\n]/, "<br>");
									}
								} else if (key == "responseText" && xhr.hasOwnProperty("responseJSON")) {
									// skip responseText when there is a responseJSON
								} else {
									// append a normal field
									ret += "<br><b>" + key + ": &nbsp</b>" + s.replace(/[\n]/, "<br>");
								}
							}
						}
					}
					return ret;
				},
				
				
				serviceSucceeded : function (xhr, status) {
					// POST was successful, but did the service also return success
					return (JSON.stringify(xhr.status).indexOf("success") == 1);
				},
				
				checkFormData : function (formdata) {
					// check whether form contains errors
					//    (1) file over 1 meg
					// Return errorHTML or null
					var errorHTML = null;
					var value;
					
					for (value of formdata.values()) {
						if (value instanceof File) {
							var file = value;
							if (file.size > 1000000) {
								errorHTML =  "<h3>File too big</h3>";
								errorHTML += "<br><b>name: &nbsp</b>" + file.name;
								errorHTML += "<br><b>size: &nbsp</b>" + file.size;
								errorHTML += "<br><br>Files over 1 Meg are unlikely to succeed in HTTP POST through this browser.";
							}
						}
					}
					
					return errorHTML;
				},
				
				
				postToEndpoint : function (endpoint, data, contentType, successCallback, optFailureCallback, optTimeout) {
					// contentType:  
					//      false - when data is a FormData()
					//      otherwise normal such as 'application/json'
					//
					// successCallback(resultSet) 
					//
					// optFailureCallback - defaults to ModalIidx.alert(title, message)
					//                    
					if (typeof optFailureCallback !== 'undefined') {
						this.userFailureCallback = optFailureCallback;
					}
					if (typeof optTimeout !== 'undefined') {
						this.timeout = optTimeout;
					}
					this.lastUrl = this.url.slice(0, this.url.lastIndexOf("/")) + '/' + endpoint;
					this.userSuccessCallback = successCallback;
					
					// check form data: (size of files)
					if (contentType === false) {
						var errorHTML = this.checkFormData(data);
						if (errorHTML) {
							this.userFailureCallback(errorHTML, true);
							return;
						}
					}
					
					$.ajax({
						url: this.lastUrl,
						type: 'POST',
						dataType: 'json',
						contentType: contentType,
						data: data,
						timeout: this.timeout,
						processData: false,  // isn't this a BUG for non form data??
					    
						success: this.successCallback.bind(this),
						error:   this.errorCallback.bind(this),
						statusCode: {
						   403: function() { 
							   // never happens
							   this.statusCodeCallback(403);        
							   alert("403 was caught.  Miracle.");  
						   }.bind(this),
						}
					});
					
				},
				
				post : function (formdata, successCallback, failureCallback) {
					// ORIGINAL multi-part form data function retained for backward compatibility
					//
					// success (xhr, status) - call to the service succeeded...Actual service might have failed
					//    Use this.isSuccess()
					// failure (xhr, status, err) - call to the service failed.  
					//    Use this.generatePostFailureMsg()
					console.log("MicroServiceInterface:post() is deprecated.  Use postToEndpoint() or other post methods.")
					this.lastUrl = this.url;
					$.ajax({
						url: this.url,
						type: 'POST',
						dataType: 'json',
						data: formdata,
						timeout: 60000,
						processData: false,
					    contentType: false,   
					    
						success: successCallback,
						error:   failureCallback,
						
					});
					
				},
				
				get : function (successCallback, failureCallback) {
					// success (xhr, status) - call to the service succeeded...Actual service might have failed
					//    Use this.isSuccess()
					// failure (xhr, status, err) - call to the service failed.  
					//    Use this.generatePostFailureMsg()
					console.log("MicroServiceInterface.get() is deprecated");
					$.ajax({
						url: this.url,
						type: 'GET',
						
						timeout: 10000,
						processData: false,
					    contentType: false,
					    
						success: successCallback,
						error:   failureCallback,
						
					});
					
				},
				
		};
	
		return MicroServiceInterface;            // return the constructor
	}
);