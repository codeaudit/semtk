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


package com.ge.research.semtk.load.utility;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/*
 * JSON handler for sparqlGraph.json files
 * 
 */
public class SparqlGraphJson {
	private JSONObject jObj = null;
	
	private SparqlConnection conn = null;
	private OntologyInfo oInfo = null;
	private ImportSpecHandler importSpec = null;
	
	public SparqlGraphJson() {
		// nothing
	}
	
	public SparqlGraphJson(JSONObject jsonObj) {
		this.jObj = jsonObj;
	}
	
	public SparqlConnection getSparqlConn() throws Exception {
		if (this.conn == null) {
			JSONObject connJson = (JSONObject) (jObj.get("sparqlConn"));
			this.conn = new SparqlConnection();
			this.conn.fromJson(connJson);
		}
		return this.conn;
	}
	
	public JSONObject getSparqlConnJson() throws Exception {
		if (jObj.containsKey("sparqlConn")) {
			return (JSONObject)jObj.get("sparqlConn");
		}
		else{
			return null;
		}
	}
	
	public JSONObject getSNodeGroupJson() {
		if (jObj.containsKey("sNodeGroup")) {
			return (JSONObject) jObj.get("sNodeGroup");
		} else {
			return null;
		}
	}
	
	public JSONObject getImportSpecJson() {
		if (jObj.containsKey("importSpec")) {
			return (JSONObject) jObj.get("importSpec");
		} else {
			return null;
		}
	}
	
	// new convenience functions not in javascript
	
	/**
	 * 
	 * @return NodeGroup - could be null
	 * @throws Exception
	 */
	public NodeGroup getNodeGroupCopy() throws Exception {
		JSONObject json = getSNodeGroupJson();
		if (json == null) {
			return null;
		} else {
			return NodeGroup.getInstanceFromJson(json);
		}
	}
	
	public String getDomain() throws Exception {
		return getSparqlConn().getDomain();
	}

	public SparqlEndpointInterface getOntologyInterface() throws Exception {
		return getSparqlConn().getOntologyInterface();
	}
	
	public SparqlEndpointInterface getDataInterface() throws Exception {
		return getSparqlConn().getDataInterface();
	}
	
	public OntologyInfo getOntologyInfo() throws Exception {
		if (oInfo == null) {
			oInfo = new OntologyInfo(getOntologyInterface(), getDomain());
		}
		return oInfo;
	}
	
	/**
	 * 
	 * @return importSpecHandler, might be NULL
	 * @throws Exception
	 */
	public ImportSpecHandler getImportSpec() throws Exception {
		JSONObject json = this.getImportSpecJson();
		if (importSpec == null && json != null) {
			importSpec =  new ImportSpecHandler(json, this.getOntologyInfo());
		}
		return importSpec;
	}
	
	// ------ end new convenience functions ----------------
	
	
	// matches javascript
	public JSONObject getMappingTabJson() {
		return getImportSpecJson();
	}
	
	// setFUNCTIONS() ported for completeness.  Not tested and I don't know where they'd be used yet
	public void setSparqlConn(SparqlConnection conn) {
		jObj.put("sparqlConn", conn.toJson());
	}
	
	public void setSNodeGroup(NodeGroup sNodeGroup) throws Exception {
		jObj.put("sNodeGroup", sNodeGroup.toJson());
	}
	
	public void setImportSpec(ImportSpecHandler importSpec) throws Exception {
		throw new Exception("setImportSpec isn't implemented");
		//jObj.put("importSpec", importSpec.toJson());
	}
	
	public void parse(String jsonString) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject jObj = (JSONObject) parser.parse(jsonString);
	}
	
}