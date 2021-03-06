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

import java.net.URI;
import java.sql.Time;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.load.transform.Transform;
import com.ge.research.semtk.load.transform.TransformInfo;
import com.ge.research.semtk.load.utility.UriResolver;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

public class ImportSpecHandler {
	
	JSONObject importspec = null; 
	
	JSONObject ngJson = null;
	HashMap<String, Integer> colIndexHash = new HashMap<String, Integer>();
	HashMap<String, Transform> transformHash = new HashMap<String, Transform>();
	HashMap<String, String> textHash = new HashMap<String, String>();
	HashMap<String, String> colNameHash = new HashMap<String, String>();
	HashMap<String, Integer> colsUsed = new HashMap<String, Integer>();    // count of cols used.  Only includes counts > 0
	
	ImportMapping mappings[] = null;
	String [] colsUsedKeys = null;
	
	UriResolver uriResolver;
	OntologyInfo oInfo;
	
	// for each node index, the mappings that do URI lookup
	HashMap<Integer, ArrayList<ImportMapping>> uriLookup = new HashMap<Integer, ArrayList<ImportMapping>>();
	HashMap<String, String> uriCache = new HashMap<String, String>();
	
	// TODO questionable design
	SparqlEndpointInterface endpoint = null;
	
	public ImportSpecHandler(JSONObject importSpecJson, JSONObject ngJson, OntologyInfo oInfo) throws Exception {
		this.importspec = importSpecJson; 
		
		// reset the nodegroup and store as json (for efficient duplication)
		NodeGroup ng = NodeGroup.getInstanceFromJson(ngJson);
		ng.reset();
		this.ngJson = ng.toJson();
		
		this.oInfo = oInfo;
		
		this.setupColNameHash(   (JSONArray) importSpecJson.get(SparqlGraphJson.JKEY_IS_COLUMNS));
		this.setupTransforms((JSONArray) importSpecJson.get(SparqlGraphJson.JKEY_IS_TRANSFORMS));
		this.setupTextHash(     (JSONArray) importSpecJson.get(SparqlGraphJson.JKEY_IS_TEXTS));
		
		String userUriPrefixValue = (String) this.importspec.get(SparqlGraphJson.JKEY_IS_BASE_URI);
		
		// check the value of the UserURI Prefix
		// LocalLogger.logToStdErr("User uri prefix set to: " +  userUriPrefixValue);
	
		this.uriResolver = new UriResolver(userUriPrefixValue, oInfo);
	}
	
	public ImportSpecHandler(JSONObject importSpecJson, ArrayList<String> headers, JSONObject ngJson, OntologyInfo oInfo) throws Exception{
		this(importSpecJson, ngJson, oInfo);
		this.setHeaders(headers);
	}

	public void setEndpoint(SparqlEndpointInterface endpoint) {
		this.endpoint = endpoint;
	}
	
	public void setHeaders(ArrayList<String> headers) throws Exception{
		int counter = 0;
		for(String h : headers){
			this.colIndexHash.put(h, counter);
			counter += 1;
		}
		
		//  bad (unfixed from original code write)
		//  setupNodes happens later because setHeaders happens later.
		//  it seems like we could/should require this at instantiation time
		this.setupNodes(     (JSONArray) this.importspec.get(SparqlGraphJson.JKEY_IS_NODES));
	}
	
	public String getUriPrefix() {
		return uriResolver.getUriPrefix();
	}
	
	/**
	 * Populate the transforms with the correct instances based on the importspec.
	 * @throws Exception 
	 */
	private void setupTransforms(JSONArray transformsJsonArr) throws Exception{
		
		if(transformsJsonArr == null){ 
			// in the event there was no transform block found in the JSON, just return.
			// thereafter, there are no transforms looked up or found.
			return;}
		
		for (int j = 0; j < transformsJsonArr.size(); ++j) {
			JSONObject xform = (JSONObject) transformsJsonArr.get(j);
			String instanceID = (String) xform.get(SparqlGraphJson.JKEY_IS_TRANS_ID); // get the instanceID for the transform
			String transType = (String) xform.get(SparqlGraphJson.JKEY_IS_TRANS_TYPE); // get the xform type 
			
			// go through all the entries besides "name", "transType", "transId" and 
			// add them to the outgoing HashMap to be sent to the transform creation.
			int totalArgs = TransformInfo.getArgCount(transType);
			
			// get the args.
			HashMap<String, String> args = new HashMap<String, String>();
			for(int argCounter = 1; argCounter <= totalArgs; argCounter += 1){
				// get the current argument
				args.put("arg" + argCounter, (String) xform.get("arg" + argCounter));
			}
			
			// get the transform instance.
			Transform currXform = TransformInfo.buildTransform(transType, instanceID, args);
			
			// add it to the hashMap.
			this.transformHash.put(instanceID, currXform);
		}
	}
	
	/**
	 * Populate the texts with the correct instances based on the importspec.
	 * @throws Exception 
	 */
	private void setupTextHash(JSONArray textsJsonArr) throws Exception{
		
		if(textsJsonArr == null){ 
			return;
		}
		
		for (int j = 0; j < textsJsonArr.size(); ++j) {
			JSONObject textJson = (JSONObject) textsJsonArr.get(j);
			String instanceID = (String) textJson.get(SparqlGraphJson.JKEY_IS_TEXT_ID); 
			String textVal = (String) textJson.get(SparqlGraphJson.JKEY_IS_TEXT_TEXT);  
			this.textHash.put(instanceID, textVal);
		}
	}

	/**
	 * Populate the texts with the correct instances based on the importspec.
	 * @throws Exception 
	 */
	private void setupColNameHash(JSONArray columnsJsonArr) throws Exception{
		
		if(columnsJsonArr == null){ 
			return;
		}
		
		for (int j = 0; j < columnsJsonArr.size(); ++j) {
			JSONObject colsJson = (JSONObject) columnsJsonArr.get(j);
			String colId = (String) colsJson.get(SparqlGraphJson.JKEY_IS_COL_COL_ID);      
			String colName = ((String) colsJson.get(SparqlGraphJson.JKEY_IS_COL_COL_NAME)).toLowerCase();  
			this.colNameHash.put(colId, colName);
		}
	}
	
	/**
	 * Sets this.colsUsed to number of times each column is used.  Skipping ZEROS.
	 * @throws Exception 
	 */
	private void setupNodes(JSONArray nodesJsonArr) throws Exception {
		NodeGroup tmpNodegroup = NodeGroup.getInstanceFromJson(this.ngJson);
		tmpNodegroup.clearOrderBy();
		ArrayList<ImportMapping> mappingsList = new ArrayList<ImportMapping>();
		// clear cols used
		colsUsed = new HashMap<String, Integer>();  
		ImportMapping mapping = null;
		
		// loop through .nodes
		for (int i = 0; i < nodesJsonArr.size(); i++){  
			
			// ---- URI ----
			JSONObject nodeJson = (JSONObject) nodesJsonArr.get(i);
			String nodeSparqlID = nodeJson.get(SparqlGraphJson.JKEY_IS_NODE_SPARQL_ID).toString();
			int nodeIndex = tmpNodegroup.getNodeIndexBySparqlID(nodeSparqlID);
			
			// look for mapping != [] that is not a URILookup
			if (nodeJson.containsKey(SparqlGraphJson.JKEY_IS_MAPPING) &&
				!nodeJson.containsKey(SparqlGraphJson.JKEY_IS_URI_LOOKUP)) {
				JSONArray mappingJsonArr = (JSONArray) nodeJson.get(SparqlGraphJson.JKEY_IS_MAPPING);
				if (mappingJsonArr.size() > 0) {
					
					mapping = new ImportMapping();
					
					// get node index
					String type = (String) nodeJson.get(SparqlGraphJson.JKEY_IS_NODE_TYPE);
					mapping.setIsEnum(this.oInfo.classIsEnumeration(type));
					mapping.setsNodeIndex(nodeIndex);
					
					setupMappingItemList(mappingJsonArr, mapping);
					mappingsList.add(mapping);
				}
			}
			
			// ---- Properties ----
			if (nodeJson.containsKey(SparqlGraphJson.JKEY_IS_MAPPING_PROPS)) {
				JSONArray propsJsonArr = (JSONArray) nodeJson.get(SparqlGraphJson.JKEY_IS_MAPPING_PROPS);
				Node snode = tmpNodegroup.getNode(nodeIndex);
				
				for (int p=0; p < propsJsonArr.size(); p++) {
					JSONObject propJson = (JSONObject) propsJsonArr.get(p);
					
					mapping = null;
					// look for mapping != [] that is not a URI Lookup
					if (propJson.containsKey(SparqlGraphJson.JKEY_IS_MAPPING)) {
						JSONArray mappingJsonArr = (JSONArray) propJson.get(SparqlGraphJson.JKEY_IS_MAPPING);	
						if (mappingJsonArr.size() > 0) {
							
							mapping = new ImportMapping();
							mapping.setsNodeIndex(nodeIndex);
							int propIndex = snode.getPropertyIndexByURIRelation((String)propJson.get(SparqlGraphJson.JKEY_IS_MAPPING_PROPS_URI_REL));
							mapping.setPropItemIndex(propIndex);
					
							setupMappingItemList(mappingJsonArr, mapping);
							
							// Add to mappingsList unless this is a URILookup mapping
							if (!propJson.containsKey(SparqlGraphJson.JKEY_IS_URI_LOOKUP)) {
								mappingsList.add(mapping);
							}
						}
					}
					
					// look for URILookup != []
					this.setupUriLookup(propJson, mapping, tmpNodegroup);
					
				}
			}
		}
		
		// create some final efficient arrays
		this.mappings = mappingsList.toArray(new ImportMapping[mappingsList.size()]);
		this.colsUsedKeys = this.colsUsed.keySet().toArray(new String[this.colsUsed.size()]);

	}
	
	/**
	 * Process a URI Lookup json array
	 * @param uriLookupJsonArr
	 * @param mapping - mapping of the node or property which owns this URI lookup, or null if none
	 * @param tmpNodegroup - example nodegroup
	 * @throws Exception
	 */
	private void setupUriLookup(JSONObject nodeOrPropJson, ImportMapping mapping, NodeGroup tmpNodegroup) throws Exception {
		
		// does this json have a URI lookup
		if (nodeOrPropJson.containsKey(SparqlGraphJson.JKEY_IS_URI_LOOKUP)) {
			JSONArray uriLookupJsonArr = (JSONArray) nodeOrPropJson.get(SparqlGraphJson.JKEY_IS_URI_LOOKUP);
			
			// is the URI lookup non-empty
			if (uriLookupJsonArr.size() > 0) {
				
				// check for empty mapping
				if (mapping == null) {
					String name;
					if (nodeOrPropJson.containsKey(SparqlGraphJson.JKEY_IS_NODE_SPARQL_ID)) {
						name = (String) nodeOrPropJson.get(SparqlGraphJson.JKEY_IS_NODE_SPARQL_ID);
					} else {
						name = (String) nodeOrPropJson.get(SparqlGraphJson.JKEY_IS_MAPPING_PROPS_URI_REL);
					}
					throw new Exception("Error in ImportSpec. Item is a URI lookup but has no mapping: " + name );
				}
				
				// loop through the sparql ID's of nodes this item is looking up
				for (int j=0; j < uriLookupJsonArr.size(); j++) {
					String lookupSparqlID = (String)uriLookupJsonArr.get(j);
					int lookupNodeIndex = tmpNodegroup.getNodeIndexBySparqlID(lookupSparqlID);
					if (lookupNodeIndex == -1) {
						throw new Exception ("Error in ImportSpec. Invalid sparqlID in URI lookup: " + lookupSparqlID );
					}
					
					// add mapping to this.uriLookup
					if (!this.uriLookup.containsKey(lookupNodeIndex)) {
						this.uriLookup.put(lookupNodeIndex, new ArrayList<ImportMapping>());
					}
					this.uriLookup.get(lookupNodeIndex).add(mapping);
				}
			}
		}
	}
	/**
	 * Put mapping items from json into an ImportMapping and update colUsed
	 * @param mappingJsonArr
	 * @param mapping
	 * @throws Exception 
	 */
	private void setupMappingItemList(JSONArray mappingJsonArr, ImportMapping mapping) throws Exception {
		
		for (int j=0; j < mappingJsonArr.size(); j++) {
			JSONObject itemJson = (JSONObject) mappingJsonArr.get(j);
			
			// mapping item
			MappingItem mItem = new MappingItem();
			mItem.fromJson(	itemJson, 
							this.colNameHash, this.colIndexHash, this.textHash, this.transformHash);
			mapping.addItem(mItem);
			
			if (itemJson.containsKey(SparqlGraphJson.JKEY_IS_MAPPING_COL_ID)) {
				// column item
				String colId = (String) itemJson.get(SparqlGraphJson.JKEY_IS_MAPPING_COL_ID);
				String colName = this.colNameHash.get(colId);
				// colsUsed
				if (this.colsUsed.containsKey(colId)) {
					this.colsUsed.put(colName, this.colsUsed.get(colName) + 1);
				} else {
					this.colsUsed.put(colName, 1);
				}
			} 
		}
	}
	
	/**
	 * Create a nodegroup from a single record (row) of data
	 */
	public NodeGroup buildImportNodegroup(ArrayList<String> record, boolean skipValidation) throws Exception{

		// create a new nodegroup copy
		NodeGroup retVal = NodeGroup.getInstanceFromJson(this.ngJson);
		retVal.clearOrderBy();
		
		if(record  == null){ throw new Exception("incoming record cannot be null for ImportSpecHandler.getValues"); }
		if(this.colIndexHash.isEmpty()){ throw new Exception("the header positions were never set for the importspechandler"); }
		
		// do URI lookups first
		for (int i=0; i < retVal.getNodeCount(); i++) {
			if (this.uriLookup.containsKey(i)) {
				String uri = this.lookupUri(i, record);
				retVal.getNode(i).setInstanceValue(uri);
			}
		}
		
		// do mappings second
		for (int i=0; i < this.mappings.length; i++) {
			ImportMapping mapping = mappings[i];
			String builtString = mapping.buildString(record);
			Node node = retVal.getNode(mapping.getsNodeIndex());
			PropertyItem propItem = null;
			
			if (mapping.isProperty()) {
				// ---- property ----
				if(builtString.length() > 0) {
					propItem = node.getPropertyItem(mapping.getPropItemIndex());
					builtString = validateDataType(builtString, propItem.getValueType(), skipValidation);						
					propItem.addInstanceValue(builtString);
				}
				
			} else {				
				
				// ---- node ----
				
				// if node has uri lookup
				if (this.uriLookup.containsKey(mapping.getsNodeIndex())) {
					LocalLogger.logToStdErr("Node has uriLookup and mapping: " + node.getSparqlID());
				}
				
				// if build string is null
				else if(builtString.length() < 1){
					node.setInstanceValue(null);
				}
				
				// use built string
				else{
					String uri = this.uriResolver.getInstanceUriWithPrefix(node.getFullUriName(), builtString);
					if (! SparqlToXUtils.isLegalURI(uri)) { throw new Exception("Attempting to insert ill-formed URI: " + uri); }
					node.setInstanceValue(uri);
				}
			}
			
		}
			
		// prune nodes that no longer belong (no uri and no properties)
		retVal.pruneAllUnused(true);
		
		// set URI for nulls
		retVal = this.setURIsForBlankNodes(retVal);
		
		return retVal;
	}
	
	/**
	 * POC ONLY
	 * Look up a URI
	 * @param lookupMappings
	 * @return
	 * @throws Exception 
	 */
	private String lookupUri(int nodeIndex, ArrayList<String> record) throws Exception {
		
		// create a new nodegroup copy:  
		// TODO this is too expensive.  cache one Nodegroup per nodeIndex and wipe constraints
		ArrayList<String> builtStrings = new ArrayList<String>();
		
		// Build the mapping results into builtStrings
		// and build a cacheKey
		StringBuffer cacheKey0 = new StringBuffer();
		cacheKey0.append(((Integer)nodeIndex).toString() + "-");
		for (ImportMapping mapping : this.uriLookup.get(nodeIndex)) {
			String builtStr = mapping.buildString(record);
			builtStrings.add(builtStr);
			cacheKey0.append(builtStr + "-");
		}
		String cacheKey = cacheKey0.toString();
				
		// return quickly if answer is already cached
		// In virtuoso, saves remarkably little time.
		// Running 1000 queries twice takes 10.5 seconds.
		// Running them once takes 8.8 seconds
		if (this.uriCache.containsKey(cacheKey)) {
			return this.uriCache.get(cacheKey);
			
		} else {
			// build a nodegroup and do the lookup
			NodeGroup tmpNodegroup = NodeGroup.getInstanceFromJson(this.ngJson);
			tmpNodegroup.clearOrderBy();
			
			// return the URI
			tmpNodegroup.getNode(nodeIndex).setIsReturned(true);
			
			// set constraint for each mapping in the uriLookup
			int i = 0;
			for (ImportMapping mapping : this.uriLookup.get(nodeIndex)) {

				ArrayList<String> valList = new ArrayList<String>();
				valList.add(builtStrings.get(i++));
				if (mapping.isNode()) {
					Node node = tmpNodegroup.getNode(mapping.getsNodeIndex());
					ValueConstraint v = new ValueConstraint(ValueConstraint.buildValuesConstraint(node, valList));
					node.setValueConstraint(v);
				} else {
					PropertyItem prop = tmpNodegroup.getNode(mapping.getsNodeIndex()).getPropertyItem(mapping.getPropItemIndex());
					ValueConstraint v = new ValueConstraint(ValueConstraint.buildValuesConstraint(prop, valList));
					prop.setValueConstraint(v);
				}
			}
			
			String query = tmpNodegroup.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 0, null);
			
			// make this thread-safe
			// TODO: questionable design just got worse.
			SparqlEndpointInterface endpointCopy = this.endpoint.copy();
			TableResultSet res = (TableResultSet) endpointCopy.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
			res.throwExceptionIfUnsuccessful();
			
			Table tab = res.getTable();
			
			if (tab.getNumRows() == 0) {
				throw new Exception("URI lookup failed.");
			} else if (tab.getNumRows() > 1) {
				throw new Exception("URI lookup found multiple URI's");
			} else {
				String ret = tab.getCell(0,0);
				this.uriCache.put(cacheKey, ret);
				return ret;
			}
		}
		
	}
	
	
	
	/**
	 * Return a pointer to every PropertyItem in ng that has a mapping in the import spec
	 * @param ng
	 * @return
	 */
	public ArrayList<PropertyItem> getMappedPropItems(NodeGroup ng) {
		// TODO: this is only used by tests?

		ArrayList<PropertyItem> ret = new ArrayList<PropertyItem>();
		
		for (int i=0; i < this.mappings.length; i++) {
			if (this.mappings[i].isProperty()) {
				ImportMapping m = this.mappings[i];
				ret.add(ng.getNode(m.getsNodeIndex()).getPropertyItem(m.getPropItemIndex()));
			}
		}
		
		return ret;
	}
	
	
	/**
	 * Get all column names that were actually used (lowercased)
	 * @return
	 */
	public String[] getColNamesUsed(){
		return this.colsUsedKeys;
	}

	
	private NodeGroup setURIsForBlankNodes(NodeGroup ng) throws Exception{
		for(Node n : ng.getNodeList()){
			if(n.getInstanceValue() == null ){
				n.setInstanceValue(this.uriResolver.getInstanceUriWithPrefix(n.getFullUriName(), UUID.randomUUID().toString()) );
			}
		}
		// return the patched results.
		return ng;
	}
	

	public static String validateDataType(String input, String expectedSparqlGraphType) throws Exception{
		return validateDataType(input, expectedSparqlGraphType, false);
	}
	
	/**
	 * Validates and in some cases modifies/reformats an input based on type
	 * @param input
	 * @param expectedSparqlGraphType - last part of the type, e.g. "float"
	 * @param skipValidation - if True, perform modifications but no validations
	 * @return - valid value
	 * @throws Exception - if invalid
	 */
	@SuppressWarnings("deprecation")
	public static String validateDataType(String input, String expectedSparqlGraphType, Boolean skipValidation) throws Exception{
		 
		 //   from the XSD data types:
		 //   string | boolean | decimal | int | integer | negativeInteger | nonNegativeInteger | 
		 //   positiveInteger | nonPositiveInteger | long | float | double | duration | 
		 //   dateTime | time | date | unsignedByte | unsignedInt | anySimpleType |
		 //   gYearMonth | gYear | gMonthDay;
		 
		// 	  added for the runtimeConstraint:
		//	  NODE_URI
		
		/**
		 *  Please keep the wiki up to date
		 *  https://github.com/ge-semtk/semtk/wiki/Ingestion-type-handling
		 */
		
		String myType = expectedSparqlGraphType.toLowerCase();
		
		// perform validations that change the input
		switch (myType) {
		case "string":
			return SparqlToXUtils.safeSparqlString(input);
		case "datetime":
			try{				 
				return Utility.getSPARQLDateTimeString(input);				 				 
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}	
		case "date":
			try{
				return Utility.getSPARQLDateString(input);				 
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
		}
		
		// efficiency circuit-breaker
		if (skipValidation) {
			return input;
		}
		
		// perform plain old validations
		switch(myType) {
		case "node_uri":
			try {
			// check that this looks like a URI
				new URI(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause: " + e.getMessage());
			}
			break;		
		case "boolean":
			try{
				Boolean.parseBoolean(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "decimal":
			try{
				Double.parseDouble(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "int":
			try{
				Integer.parseInt(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "integer":
			try {
				Integer.parseInt(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "long":
			try {
				Long.parseLong(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "float":
			try{
				Float.parseFloat(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "double":
			try {
				Double.parseDouble(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "time":
			try{
				Time.parse(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "negativeinteger":
			try{
				int test = Integer.parseInt(input);
				if(test >= 0){
					throw new Exception("value in model is negative integer. non-negative integer given as input");
					}
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "nonNegativeinteger":
			try{
				int test = Integer.parseInt(input);
				if(test < 0){
					throw new Exception("value in model is nonnegative integer. negative integer given as input");
				}
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "positiveinteger":
			try{
				int test = Integer.parseInt(input);
				if(test <= 0){
					throw new Exception("value in model is positive integer. integer <= 0 given as input");
				} 
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "nonpositiveinteger":
			try{
				int test = Integer.parseInt(input);
				if(test > 0){
					throw new Exception("value in model is nonpositive integer. integer > 0 given as input");
				}
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;

		case "duration":
			// not sure how to check this one. this might not match the expectation from SADL
			try{
				Duration.parse(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "unsignedbyte":
			try{
				Byte.parseByte(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "unsignedint":
			try{
				Integer.parseUnsignedInt(input);
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "anySimpleType":
			// do nothing. 
			break;
		case "gyearmonth":
			try{
				String[] all = input.split("-");
				// check them all
				if(all.length != 2){ throw new Exception("year-month did not have two parts."); }
				if(all[0].length() != 4 && all[1].length() != 2){ throw new Exception("year-month format was wrong. " + input + " given was not YYYY-MM"); }
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "gyear":
			try{
				if(input.length() != 4){ throw new Exception("year-month format was wrong. " + input + " given was not YYYY-MM"); }
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;
		case "gmonthday":
			try {
				String[] all = input.split("-");
				// check them all
				if(all.length != 2){ throw new Exception("month-day did not have two parts."); }
				if(all[0].length() != 2 && all[1].length() != 2){ throw new Exception("month-day format was wrong. " + input + " given was not MM-dd"); }
			}
			catch(Exception e){
				throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			}
			break;

		default:
				// unknown types slip through un-validated.
		}
		
		return input;
	}
}
