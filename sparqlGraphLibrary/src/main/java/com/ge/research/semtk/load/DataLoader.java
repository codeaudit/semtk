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


package com.ge.research.semtk.load;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.DataSetExhaustedException;
import com.ge.research.semtk.load.utility.DataToModelTransformer;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Imports a dataset into a triple store.
 **/
public class DataLoader {
	// actually orchestrates the loading of data from a dataset based on a template.

	NodeGroup master = null;
	ArrayList<NodeGroup> nodeGroupBatch = new ArrayList<NodeGroup>();     // stores the subgraphs to be loaded.  
	SparqlEndpointInterface endpoint = null;
	DataToModelTransformer dttmf = null; 
	int batchSize = 1;	// maximum batch size for insertion to the triple store
	String username = null;
	String password = null;
	OntologyInfo oInfo = null;
	
	int MAX_WORKER_THREADS = 10;
	
	public final static String FAILURE_CAUSE_COLUMN_NAME = "Failure Cause";
	public final static String FAILURE_RECORD_COLUMN_NAME = "Failure Record Number";

	int totalRecordsProcessed = 0;
	
	public DataLoader(){
		// default and does nothing special 
	}
	
	public DataLoader(SparqlGraphJson sgJson, int bSize) throws Exception {
		// take a json object which encodes the node group, the import spec and the connection in one package. 
		
		this.batchSize = bSize;
		
		this.endpoint = sgJson.getSparqlConn().getInsertInterface();
		
		LocalLogger.logToStdOut("Load to graph " + endpoint.getDataset() + " on " + endpoint.getServerAndPort());
		
		this.oInfo = sgJson.getOntologyInfo();				
		this.master = sgJson.getNodeGroup(this.oInfo);
		this.dttmf = new DataToModelTransformer(sgJson, this.batchSize, endpoint);		
	}
	

	public DataLoader(SparqlGraphJson sgJson, int bSize, Dataset ds, String username, String password) throws Exception{
		this(sgJson, bSize);
		this.setCredentials(username, password);
		this.setDataset(ds);
		this.validateColumns(ds);
		
	}
	
	public DataLoader(JSONObject json, int bSize) throws Exception {
		this(new SparqlGraphJson(json), bSize);
	}
	
	public DataLoader(JSONObject json, int bSize, Dataset ds, String username, String password) throws Exception{
		this(new SparqlGraphJson(json), bSize, ds, username, password);
	}
	
	public String getDatasetGraphName(){
		return this.endpoint.getDataset();
	}
	
	public int getTotalRecordsProcessed(){
		return this.totalRecordsProcessed;
	}
	
	private void validateColumns(Dataset ds) throws Exception {
		// validate that the columns specified in the template are present in the dataset
		String[] colNamesToIngest = dttmf.getImportColNames();   // col names from JSON		
		ArrayList<String> colNamesInDataset = ds.getColumnNamesinOrder();
		for(String c: colNamesToIngest){
			if(!colNamesInDataset.contains(c)){
				ds.close();  // close the dataset (e.g. if Oracle, will close the connection)
				
				// log some info on the bad column before throwing the exception.
				LocalLogger.logToStdErr("column " + c + " not found in the data set. the length of the column name was " + c.length() + " . the character codes are as follows:");
				for(int i = 0; i < c.length(); i += 1){
					char curr = c.charAt(i);
					System.err.print((int) curr + " ");
				}
				LocalLogger.logToStdErr("");
				// available columns list 
				LocalLogger.logToStdErr("available columns are: ");
				for(String k : colNamesInDataset ){
					System.err.print(k + " (");
					for(int m = 0; m < k.length(); m += 1){
						char curr = k.charAt(m);
						System.err.print((int) curr + " ");
					}
					LocalLogger.logToStdErr(")" );
				}
				
				throw new Exception("Column '" + c + "' not found in dataset. Available columns are: " + colNamesInDataset.toString());
			}
		}
	}
	public void setCredentials(String user, String pass){
		this.endpoint.setUserAndPassword(user, pass);
	}
	
	public void setDataset(Dataset ds) throws Exception{
		if(this.dttmf == null){
			throw new Exception("There was no DAta to model TRansform initialized when setting dataset.");
		}
		else{
			this.dttmf.setDataset(ds);
		}
		
	}
	
	public void setBatchSize(int bSize){
		// set the maximum batch size for insertion to the triple store
		this.batchSize = bSize;
	}	
	
	public void setRetrivalBatchSize(int rBatchSize){
		this.dttmf.setBatchSize(rBatchSize);
	}
	
	
	public int importData(Boolean checkFirst) throws Exception{

		// check the nodegroup for consistency before continuing.
		
		LocalLogger.logToStdErr("about to validate against model.");
		this.master.validateAgainstModel(this.oInfo);
		LocalLogger.logToStdErr("validation completed.");
		
		Boolean dataCheckSucceeded = true;
		this.totalRecordsProcessed = 0;	// reset the counter.
		
		// preflight the data to make sure everything seems okay before a load.
		int totalPreflightRecordsChecked = 0;
		if(checkFirst){
			// try structuring around model but do not load. 	
			while(true){
				try{					
					this.dttmf.getNextNodeGroups(1, false);
					totalPreflightRecordsChecked++;
				}
				catch(DataSetExhaustedException e){
					break;
				}
			}
			// inspect the transformer to determine if the checks succeeded
			Table errorReport = this.dttmf.getErrorReport();
			if(errorReport.getRows().size() != 0){
				dataCheckSucceeded = false;
			}
		}
				
		if (dataCheckSucceeded) {
			this.dttmf.resetDataSet();
			// orchestrate the retrieval of new nodegroups and the flushing of
			// that data
			System.out.print("Records processed:");
			long timeMillis = System.currentTimeMillis();  // use this to report # recs loaded every X sec
			
			ArrayList<IngestionWorkerThread> wrkrs = new ArrayList<IngestionWorkerThread>();
			
			while (true) {
				// get the next set of records from the data set.
				ArrayList<ArrayList<String>> nextRecords = null;
		
				try{
					nextRecords = this.dttmf.getNextRecordsFromDataSet();
				}catch(Exception e){ break; } // record set exhausted
				
				if(nextRecords == null || nextRecords.size() == 0 ){ break; }
				
				// spin up a thread to do the work.
				if(wrkrs.size() < this.MAX_WORKER_THREADS){
					// spin up the thread and do the work. 
					IngestionWorkerThread neo = new IngestionWorkerThread(this.endpoint.copy(), this.dttmf, nextRecords, this.oInfo, checkFirst);
					wrkrs.add(neo);
					neo.start();
					this.totalRecordsProcessed += nextRecords.size();
				}
				// wait until we free up.
				if(wrkrs.size() == this.MAX_WORKER_THREADS){
					for(int counter = 0; counter < wrkrs.size(); counter++){
						wrkrs.get(counter).join();
					}
					// reset it all 
					wrkrs.clear();
				}
			}
			// await any still running threads:
			for(int counter = 0; counter < wrkrs.size(); counter++){
				wrkrs.get(counter).join();
			}

		}
		LocalLogger.logToStdOut("..." + this.totalRecordsProcessed + "(DONE)");
		this.dttmf.closeDataSet();			// close all connections and clean up
		return this.totalRecordsProcessed;  // report.
	}
	
	public void insertToTripleStore() throws Exception{
		// take the values from the current collection of node groups and then send them off to the store. 

		String query =  NodeGroup.generateCombinedSparqlInsert(this.nodeGroupBatch, this.oInfo);

		this.endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
	}
	
	/**
	 * Returns a table containing the failed data rows, along with failure cause and row number.
	 */
	public Table getLoadingErrorReport(){
		return this.dttmf.getErrorReport();
	}
	
	/**
	 * Returns an error report giving the row number and failure cause for each failure.
	 */
	public String getLoadingErrorReportBrief(){
		String s = "";
		Table errorReport = this.dttmf.getErrorReport();
		int failureCauseIndex = errorReport.getColumnIndex(FAILURE_CAUSE_COLUMN_NAME);
		int failureRowIndex = errorReport.getColumnIndex(FAILURE_RECORD_COLUMN_NAME);
		ArrayList<ArrayList<String>> rows = errorReport.getRows();
		for(ArrayList<String> row:rows){
			s += "Error in row " + row.get(failureRowIndex) + ": " + row.get(failureCauseIndex) + "\n";
		}
		return s;
	}
	

}
