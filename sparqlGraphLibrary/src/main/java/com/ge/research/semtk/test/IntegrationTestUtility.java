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

package com.ge.research.semtk.test;

import com.ge.research.semtk.utility.Utility;

/**
 * Utility class for configuring integration tests.
 * 
 * NOTE: This class cannot be put in under src/test/java because it must remain accessible to other projects.
 */
public class IntegrationTestUtility {
	
	// property file with integration test configurations
	public static final String INTEGRATION_TEST_PROPERTY_FILE = "src/test/resources/integrationtest.properties";

	
	// protocol for all services
	public static String getServiceProtocol() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.protocol");
	}
	
	// sparql endpoint
	public static String getSparqlServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlendpoint.server");
	}
	public static String getSparqlServerType() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlendpoint.type");
	}
	public static String getSparqlServerUsername() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlendpoint.username");
	}
	public static String getSparqlServerPassword() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlendpoint.password");
	}
	
	// sparql query service
	public static String getSparqlQueryServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlqueryservice.server");
	}
	public static int getSparqlQueryServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlqueryservice.port")).intValue();	
	}	
	
	// ingestion service
	public static String getIngestionServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.ingestionservice.server");
	}
	public static int getIngestionServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.ingestionservice.port")).intValue();	
	}	
		
	// status service
	public static String getStatusServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.statusservice.server");
	}
	public static int getStatusServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.statusservice.port")).intValue();
	}
	
	// results service
	public static String getResultsServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.resultsservice.server");
	}
	public static int getResultsServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.resultsservice.port")).intValue();
	}
	
	// dispatch service
	public static String getDispatchServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.dispatchservice.server");
	}
	public static int getDispatchServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.dispatchservice.port")).intValue();
	}	
	
	// RDB query generator service
	public static String getRDBQueryGeneratorServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.rdbquerygenservice.server");
	}
	public static int getRDBQueryGeneratorServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.rdbquerygenservice.port")).intValue();
	}
		
	// Hive service
	public static String getHiveServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.hiveservice.server");
	}
	public static int getHiveServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.hiveservice.port")).intValue();
	}
	
	// nodegroup store service
	public static String getNodegroupStoreServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.nodegroupstoreservice.server");
	}
	public static int getNodegroupStoreServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.nodegroupstoreservice.port")).intValue();
	}
	
	// nodegroup execution service
	public static String getNodegroupExecutionServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.nodegroupexecution.server");
	}
	public static int getNodegroupExecutionServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.nodegroupexecution.port")).intValue();
	}
	
	// Hive
	public static String getHiveServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.hive.server");
	}
	public static int getHivePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.hive.port")).intValue();
	}
	public static String getHiveUsername() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.hive.username");
	}
	public static String getHivePassword() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.hive.password");
	}
	public static String getHiveDatabase() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.hive.database");
	}
	
	// Oracle
	public static String getOracleServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.oracle.server");
	}
	public static int getOraclePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.oracle.port")).intValue();
	}
	public static String getOracleUsername() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.oracle.username");
	}
	public static String getOraclePassword() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.oracle.password");
	}
	public static String getOracleDatabase() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.oracle.database");
	}	
	
}
