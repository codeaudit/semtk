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

define([	// properly require.config'ed
        	 'sparqlgraph/js/mappingitem',
              	
     		// shimmed
        	// 'logconfig',
		],

		function (MappingItem) {
		
			/**
			 * An import mapping from columns, texts, and transforms to a class or property in the nodegroup.
		     * @description <font color="red">Users of {@link SemtkAPI} should not call this constructor.</font><br>Use {@link SemtkImportAPI#getMappings} or {@link SemtkImportAPI#getMapping} instead
			 * @alias ImportMapping
			 * @class
			 */
			var ImportMapping = function (node, propItem) {
				// params are purposely not documented for jsdoc.  Not designed for the API.
				// node: SemanticNode
				// propItem: PropertyItem
				this.node = node;
				this.propItem = propItem;
				this.itemList = [];
			};
			
			ImportMapping.staticGenUniqueKey = function(sparqlId, propUri) {
				return sparqlId + "." + (propUri != null ? propUri : "");
			};
			
			ImportMapping.prototype = {
				//
				// NOTE any methods without jsdoc comments is NOT meant to be used by API users.
				//      These methods' behaviors are not guaranteed to be stable in future releases.
				//
					
				/**
				 * @description Add a mapping item
				 * @param {MappingItem} item           item to add
				 * @param {MappingItem} insertBefore   insert here.  If null then add to end.
				 */
				addItem : function (item, insertBefore) {
					if (insertBefore == null) {
						this.itemList.push(item);
					} else {
						var pos = this.itemList.indexOf(insertBefore);
						this.itemList.splice(pos, 0, item);
					}
					item.incrUse(1);
				},
				
				/**
				 * @description clears all mapping items
				 */
				clearItems : function () {
					// clears items without any regard for the ImportSpec
					this.itemList = [];
				},
				
				/**
				 * @description delete a given mapping item
				 * @param {MappingItem} item the mapping item to delete
				 * @throws exception if item is invalid
				 */
				delItem : function (item) {
					var i = this.itemList.indexOf(item);
					if (i < 0) { kdlLogAndThrow("Internal error in ImportRow.delItem().  Item isn't in this row.");}
					
					item.incrUse(-1);
					this.itemList.splice(i, 1);
				},
				
				fromJsonNode : function (jNode, idHash, iSpec) {
					if (! jNode.hasOwnProperty("sparqlID")) { kdlLogAndThrow("Internal error in ImportRow.fromJsonNode().  No sparqlID field.");}
					
					var node = iSpec.nodegroup.getNodeBySparqlID(jNode.sparqlID);
					if (! node) {kdlLogAndThrow("ImportMapping.fromJsonNode() can't find node in nodegroup: " + jNode.sparqlID); }
					this.node = node;
					this.propItem = null;
					
					if (! jNode.hasOwnProperty("mapping")) { kdlLogAndThrow("Internal error in ImportRow.fromJsonNode().  No mapping field.");}
					this.itemsFromJson(jNode.mapping, idHash);
				},
				
				fromJsonProp : function (jProp, node, idHash) {
					this.node = node;   
					
					if (! jProp.hasOwnProperty("URIRelation")) { kdlLogAndThrow("Internal error in ImportRow.fromJsonProp().  No URIRelation field.");}
					var propItem = node.getPropertyByURIRelation(jProp.URIRelation);
					if (! propItem) { kdlLogAndThrow("ImportMapping.fromJsonProp() can't find property in nodegroup: " + node.getSparqlID() + "->" + jProp.URIRelation); }

					if (! jProp.hasOwnProperty("mapping")) { kdlLogAndThrow("Internal error in ImportRow.fromJsonProp().  No mapping field.");}
					this.itemsFromJson(jProp.mapping, idHash);
				},
				
				getNode : function () {
					return this.node;
				},
				
				getPropItem : function () {
					return this.propItem;
				},
				
				/**
				 * @description Get the MappingItems for this ImportMapping
				 * @returns {MappingItem[]} ordered list of items
				 */
				getItemList : function () {
					return this.itemList;
				},
				
				genUniqueKey : function () {
					// key is unique to a nodegroup
					return ImportMapping.staticGenUniqueKey(this.getNodeSparqlId(), this.getPropUri());
				},
				
				itemsFromJson : function (jMapping, idHash) {
					for (var i=0; i < jMapping.length; i++) {
						var item = new  MappingItem(null, null, null);
						item.fromJson(jMapping[i], idHash);
						this.itemList.push(item);
					}
				},
				
				toJson : function (idHash) {
					var ret = {};
					if (this.propItem == null) {
						// do node row
						ret.sparqlID = this.node.getSparqlID();
						ret.type = this.node.getURI();
					} else {
						// prop row
						ret.URIRelation = this.propItem.getUriRelation();
					}
					
					//mapping
					ret.mapping = [];
					for (var i=0; i < this.itemList.length; i++) {
						ret.mapping.push(this.itemList[i].toJson(idHash));
					}
					
					return ret;
				},
				
				getNodeSparqlId : function() {
					return this.node.getSparqlID();
				},
				
				getPropUri : function() {
					return this.propItem ? this.propItem.getUriRelation() : null;
				},
				
				// OLD

				getItemListJson_OLD : function () {
					// skip line if user didn't enter anything
					if (this.itemList.length === 0) return null;

					// parse the cell of things entered by the user
					var rowJson = [];
					for (var j=0; j < this.itemList.length; j++) {
						rowJson.push(this.itemList[j].toJson());
					}

					return rowJson;
				},

				

			};
			return ImportMapping;            
	}
);