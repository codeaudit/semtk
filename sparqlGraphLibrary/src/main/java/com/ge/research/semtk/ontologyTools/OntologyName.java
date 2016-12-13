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


package com.ge.research.semtk.ontologyTools;

import com.ge.research.semtk.ontologyTools.OntologyName;

public class OntologyName {

	private String name = "";
	
	public OntologyName(String fullName) {
		this.name = fullName;
	}
	
	public String getLocalName(){
		String[] retval = this.name.split("#");

		if(retval.length > 1){
			return retval[1];
		}
		else{
			return retval[0];
		}
	}
	
	public String getFullName(){
		return this.name;
	}

	public String getNamespace(){
		String[] retval = this.name.split("#");

		if(retval.length > 1){
			return retval[0];
		}
		else{
			return ""; // there was no namespace. 
		}
	}
	
	public Boolean equals(OntologyName oName){
		// if both have the same full name, they are the same. 
		return(this.name == oName.name);
	}
	
	public Boolean isInDomain(String domain){
		int i = this.name.indexOf(domain);
		return(i == 0);
	}
}