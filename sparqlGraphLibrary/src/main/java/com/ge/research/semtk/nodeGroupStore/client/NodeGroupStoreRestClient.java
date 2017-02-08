package com.ge.research.semtk.nodeGroupStore.client;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;

public class NodeGroupStoreRestClient extends RestClient {

	@Override
	public void buildParametersJSON() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEmptyResponse() throws Exception {
		// TODO Auto-generated method stub
		
	}

	public NodeGroupStoreRestClient (NodeGroupStoreConfig config) {
		this.conf = config;
	}
	
	public TableResultSet executeGetNodeGroupById(String id) throws Exception{
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupById");
		this.parametersJSON.put("id", id);
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			JSONObject tblWrapper = (JSONObject)jobj.get("table");
			
			System.err.println(tblWrapper);
						
			Table tbl = Table.fromJson((JSONObject)tblWrapper.get("@table"));
			retval.addResults(tbl);
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
		}
		
		return retval;
	}
	
	public TableResultSet executeGetNodeGroupList() throws Exception {
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupList");
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			JSONObject tblWrapper = (JSONObject)jobj.get("table");
			
			Table tbl = Table.fromJson((JSONObject)tblWrapper.get("@table"));
			retval.addResults(tbl);
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
		}
		
		return retval;
		
	}
	
	public TableResultSet executeGetNodeGroupRuntimeConstraints(String nodeGroupId) throws Exception {
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupRuntimeConstraints");		
		this.parametersJSON.put("id", nodeGroupId);
		
		try{
			JSONObject jobj = (JSONObject) this.execute();
			JSONObject tblWrapper = (JSONObject)jobj.get("table");
			
			Table tbl = Table.fromJson((JSONObject)tblWrapper.get("@table"));
			retval.addResults(tbl);
			retval.readJson(jobj);
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
		}
		
		return retval;		
	}
	
	public SimpleResultSet executeStoreNodeGroup(String proposedId, String comments, JSONObject nodeGroupJSON) throws Exception {
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroupStore/getNodeGroupById");
		this.parametersJSON.put("id", proposedId);
		this.parametersJSON.put("name", proposedId);
		this.parametersJSON.put("comments", comments);
		this.parametersJSON.put("jsonRenderedNodeGroup", nodeGroupJSON.toJSONString() );
		
		try{
		
			TableResultSet ret = new TableResultSet((JSONObject) this.execute());
			if(ret.getTable().getNumRows() >= 1){
				// this is a problem as this already exists. 
				throw new Exception ("executeStoreNodeGroup :: nodegrouop with ID (" + proposedId + ") already exists. no work will be performed.");
			}
					
			else{
				this.parametersJSON.remove("id");
				System.err.println("existence check succeeded. proceeding to insert node group: " + proposedId);
				
				// perform actual insertion.
				conf.setServiceEndpoint("nodeGroupStore/storeNodeGroup");
				JSONObject interim = (JSONObject) this.execute();
				retval = SimpleResultSet.fromJson( interim );
			}
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
			this.parametersJSON.remove("name");
			this.parametersJSON.remove("jsonRenderedNodeGroup");
			this.parametersJSON.remove("comments");
		}
		
		return retval;				
	}
	
	public SimpleResultSet deleteStoredNodeGroup(String nodeGroupID) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroupStore/deleteStoredNodeGroup");
		this.parametersJSON.put("id", nodeGroupID);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("id");
		}

		return retval;
	}
}
