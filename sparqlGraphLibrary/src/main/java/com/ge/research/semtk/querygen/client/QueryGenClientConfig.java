/**
 ** Copyright 2016 General Electric Company
 **
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


package com.ge.research.semtk.querygen.client;

import com.ge.research.semtk.services.client.RestClientConfig;

/**
 * Extends abstract class (but adds no functionality)
 */
public class QueryGenClientConfig extends RestClientConfig {

	public QueryGenClientConfig(String serviceProtocol, String serviceServer, int servicePort, String serviceEndpoint) throws Exception {
		super(serviceProtocol, serviceServer, servicePort, serviceEndpoint);
		// TODO Auto-generated constructor stub
	}

}