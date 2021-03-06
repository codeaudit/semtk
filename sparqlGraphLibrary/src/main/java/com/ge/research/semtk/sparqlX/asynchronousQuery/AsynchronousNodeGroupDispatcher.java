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

package com.ge.research.semtk.sparqlX.asynchronousQuery;

import java.io.IOException;
import java.net.ConnectException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * @author Justin
 *
 * Open source dispatcher.
 */
public class AsynchronousNodeGroupDispatcher extends AsynchronousNodeGroupBasedQueryDispatcher {
	
	protected static final int MAX_NUMBER_SIMULTANEOUS_QUERIES_PER_USER = 50;  // maybe move this to a configured value?
	

	
	/**
	 * create a new instance of the AsynchronousNodeGroupExecutor.
	 * @param encodedNodeGroup
	 * @throws Exception
	 */
	public AsynchronousNodeGroupDispatcher(String jobId, SparqlGraphJson sgJson, ResultsClient rClient, StatusClient sClient, SparqlQueryClient queryClient, boolean unusedFlag) throws Exception{
		super(jobId, sgJson, rClient, sClient, queryClient, unusedFlag);
		
	}
	
	/**
	 * return the JobID. the clients will need this.
	 * @return
	 */
	public String getJobId(){
		return this.jobID;
	}
	
	
	@Override
	public String getConstraintType() {
		// not supported in this sub-type.
		return null;
	}

	@Override
	public String[] getConstraintVariableNames() throws Exception {
		// not supported in this sub-type.
		return null;
	}

	/**
	 * Simplest form of dispatcher execute:  get SPARQL and execute it.
	 */
	@Override
	public TableResultSet execute(Object ExecutionSpecificConfigObject, DispatcherSupportedQueryTypes qt, String targetSparqlID) throws Exception {
	TableResultSet retval = null; // expect this to get instantiated with the appropriate subclass.		
		
		try{
			String sparqlQuery = this.getSparqlQuery(qt, targetSparqlID);
			retval = this.executePlainSparqlQuery(sparqlQuery, qt);
		}
		catch(Exception e){
			// something went awry. set the job to failure. 
			this.updateStatusToFailed(e.getMessage());
			LocalLogger.printStackTrace(e);
			throw new Exception("Query failed: " + e.getMessage() );
		}
		// ship the results.
		return retval;		
	}

}
