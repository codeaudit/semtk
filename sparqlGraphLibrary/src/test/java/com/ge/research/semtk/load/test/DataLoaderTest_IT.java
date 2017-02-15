package com.ge.research.semtk.load.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Hashtable;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.test.TestGraph;

public class DataLoaderTest_IT {

	private static final String DELETE_URI_FMT = "delete { ?x ?y ?z.} where { ?x ?y ?z FILTER (?x = <%s> || ?z = <%s>).\n }";
	private static final String SELECT_URI_TRIPLES_FMT = "select distinct ?x ?y ?z where { ?x ?y ?z FILTER (?x = <%s> || ?z = <%s>).\n }";	
	
	@Test
	public void testOriginal() throws Exception {
		Dataset ds = new CSVDataset("src/test/resources/testTransforms.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// test
		DataLoader dl = new DataLoader(sgJson, 2, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}
		assertEquals(dl.getTotalRecordsProcessed(), 3);
	}	
	
	@Test
	public void testTransforms() throws Exception {
		// Paul
		// Test that a transform is applied properly to a column value but NOT
		// to the adjacent text

		// set up the data
		String contents = "cell,size in,lot,material,guy,treatment\n"
				+ "abcde_test,,,,,\n";
		Dataset ds = new CSVDataset(contents, true);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// calculate expected uri after applying transform. Capitalize all the
		// "E"s in the column value but not the text "Cell_"
		String prefix = sgJson.getImportSpec().getUriPrefix();
		String uri = prefix + "Cell_abcdE_tEst";

		// delete uri if left over from previous tests
		deleteUri(uri);

		// import
		DataLoader dl = new DataLoader(sgJson, 2, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}

		// look for triples
		confirmUriExists(uri);
	}
	
	@Test
	public void testBadUri() throws Exception {
		// Paul
		// Test uri with spaces and <> to make sure it is escaped properly

		// set up the data
		String contents = "Battery,Cell,birthday,color\n"
				+ "<contains space and brackets>,cellA,01/01/1966,red\n";

		//String CORRECT = "Cell_%3Ccontains%20spacE%20and%20brackEts%3E";
		String owlPath = "src/test/resources/sampleBattery.owl";
		String jsonPath = "src/test/resources/sampleBattery.json";
		Dataset ds = new CSVDataset(contents, true);

		// get json
		TestGraph.clearGraph();
		TestGraph.uploadOwl(owlPath);
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath);

		// calculate expected uri after applying transform. Capitalize all the
		// "E"s in the column value but not the text "Cell_"
		//String prefix = sgJson.getImportSpec().getUriPrefix();

		// import
		DataLoader dl = new DataLoader(sgJson, 2, ds, TestGraph.getUsername(), TestGraph.getPassword());

		int records = dl.importData(true);
		assertTrue(records == 0);
		assertTrue(dl.getLoadingErrorReport().getRow(0).get(4).contains("ill-formed URI"));
	}	
	
	@Test
	public void testMessyString1() throws Exception {
		// Test string with \r and \n
		String uri = "testMessyString"; // avoid the uppercasing transform by using uppercase
		String pastelot = "This is a messy\n string \r\n line3\n";
		String contents = "cell,size in,lot,material,guy,treatment\n" + uri 	+ ",,\"" + pastelot + "\",,,\n";

		Dataset ds = new CSVDataset(contents, true);

		// get json
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// calculate expected uri after applying transform. Capitalize all the "E"s in the column value but not the text "Cell_"
		String prefix = sgJson.getImportSpec().getUriPrefix();
		uri = prefix + "Cell_" + uri.replaceAll("e", "E");

		// delete uri if left over from previous tests
		deleteUri(uri);

		// import
		DataLoader dl = new DataLoader(sgJson, 2, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}

		// look for triples
		NodeGroup nodegroup = sgJson.getNodeGroupCopy();
		constrainUri(nodegroup, "Cell", uri);
		returnProp(nodegroup, "Cell", "cellId");
		returnProp(nodegroup, "ScreenPrinting", "pasteLot");

		String query = nodegroup.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, null, null, false);

		Table tab = runQuery(query);
		if (tab.getNumRows() < 1) {
			throw new Exception("No triples were found for uri: " + uri);
		}

		// check first pasteLot
		String answer = tab.getRow(0).get(1);
		if (!answer.equals(pastelot)) {
			fail(String.format("Inserted wrong string: %s expecting %s ", answer, pastelot));
		}
	}
	
	
	@Test
	public void testMessyStringsInCsv() throws Exception {
		// Reads strings out of testStrings.csv
		//
		// Adding more test cases:
		// Fill in only the two columns already used
		// Make sure "cell" starts with "testStrings" and is unique
		// Then "lot" should be the string to be tested
		// get the csv
		Dataset ds = new CSVDataset("src/test/resources/testStrings.csv", false);
		CSVDataset csv = new CSVDataset("src/test/resources/testStrings.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// import
		DataLoader dl = new DataLoader(sgJson, 2, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}

		// get all the answers
		String query = "prefix testconfig:<http://research.ge.com/spcell#> \n"
				+ " \n" + "select distinct ?cellId ?pasteLot where { \n"
				+ "   ?Cell a testconfig:Cell. \n"
				+ "   ?Cell testconfig:cellId ?cellId . \n"
				+ "      FILTER regex(?cellId, \"^testStrings\") . \n" + " \n"
				+ "   ?Cell testconfig:screenPrinting ?ScreenPrinting. \n"
				+ "      ?ScreenPrinting testconfig:pasteLot ?pasteLot . \n"
				+ "}";

		Table tab = runQuery(query);
		if (tab.getNumRows() < 1) {
			throw new Exception("No triples were found for uri: "); // + uri);
		}

		// put all expected results into a hashtable
		Hashtable<String, String> correct = new Hashtable<String, String>();
		ArrayList<ArrayList<String>> rows = csv.getNextRecords(1000);
		for (int i = 0; i < rows.size(); i++) {
			correct.put(rows.get(i).get(0), rows.get(i).get(2));
		}

		// loop through actual results
		for (int i = 0; i < tab.getNumRows(); i++) {
			String key = tab.getRow(i).get(0);
			String actual = tab.getRow(i).get(1);
			String expect = correct.get(key);

			// these substitutions are expected
			expect = expect.replace("\\n", "\n");
			expect = expect.replace("\\t", "\t");

			if (!actual.equals(expect)) {
				fail(String.format(
						"Bad string retrieved.  Expecting '%s' Found '%s'",	expect, actual));
			}
		}
	}
	

	@Test
	public void testCaseTransform() throws Exception {
		// Paul
		// Test string with \r and \n

		String uri = "testCaseTransform";
		String guy = "fred";
		String stuff = "STUFF";
		// set up the data
		String contents = "cell,size in,lot,material,guy,treatment\n" + String.format("%s,,,%s,%s,", uri, stuff, guy);

		Dataset ds = new CSVDataset(contents, true);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// calculate expected uri after applying transform. Capitalize all the
		// "E"s in the column value but not the text "Cell_"
		String prefix = sgJson.getImportSpec().getUriPrefix();
		uri = prefix + "Cell_" + uri.replaceAll("e", "E");

		// delete uri if left over from previous tests
		deleteUri(uri);

		// import
		DataLoader dl = new DataLoader(sgJson, 2, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}

		// look for triples

		NodeGroup nodegroup = sgJson.getNodeGroupCopy();
		returnProp(nodegroup, "Cell", "cellId");
		returnProp(nodegroup, "ScreenPrinting", "pasteVendor");
		returnProp(nodegroup, "ScreenPrinting", "pasteMaterial");
		constrainUri(nodegroup, "Cell", uri);

		String query = nodegroup.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, null, null, false);

		Table tab = runQuery(query);
		if (tab.getNumRows() < 1) {
			throw new Exception("No triples were found for uri: " + uri);
		}

		// check first pasteVendor
		String answer;
		guy = guy.toUpperCase();
		stuff = stuff.toLowerCase();
		answer = tab.getRow(0).get(tab.getColumnIndex("pasteVendor"));
		if (!answer.equals(guy)) {
			fail(String.format("Inserted wrong string: %s expecting %s ", answer, guy));
		}
		answer = tab.getRow(0).get(tab.getColumnIndex("pasteMaterial"));
		if (!answer.equals(stuff)) {
			fail(String.format("Inserted wrong string: %s expecting %s ", answer, stuff));
		}
	}	
	
	
	@Test
	public void testGraphLoadBattery() throws Exception {
		SparqlGraphJson sgJson = TestGraph.initGraphWithData("sampleBattery");
		CSVDataset csvDataset = new CSVDataset("src/test/resources/sampleBattery.csv", false);
		DataLoader dl = new DataLoader(sgJson, 5, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		assertEquals(dl.getTotalRecordsProcessed(), 4);
	}
	

	@Test
	public void testGraphLoadBadEnum() throws Exception {
		String csvPath = "src/test/resources/sampleBatteryBadColor.csv";
		String jsonPath = "src/test/resources/sampleBattery.json";
		String owlPath = "src/test/resources/sampleBattery.owl";

		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath);
		CSVDataset csvDataset = new CSVDataset(csvPath, false);
		TestGraph.clearGraph();
		TestGraph.uploadOwl(owlPath);

		DataLoader dl = new DataLoader(sgJson, 5, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		String report = dl.getLoadingErrorReportBrief();

		// make sure there is a complaint about the bad color
		assertTrue(report.contains("whiteish-yellow"));
		assertTrue(dl.getTotalRecordsProcessed() == 0);
	}
	
	
	
	
	
	
	// TODO: fails if we don't set SparqlID
	// TODO: fails if SparqlID doesn't start with "?"
	private void returnProp(NodeGroup nodegroup, String node, String prop) {
		PropertyItem cellId = nodegroup.getNodeBySparqlID("?" + node).getPropertyByKeyname(prop);
		cellId.setSparqlID("?" + prop);
		cellId.setIsReturned(true);
	}

	private void constrainUri(NodeGroup nodegroup, String nodeUri, String constraintUri) {
		Node node = nodegroup.getNodeBySparqlID("?" + nodeUri);
		ValueConstraint v = new ValueConstraint(String.format("VALUES ?Cell { <%s> } ", constraintUri));
		node.setValueConstraint(v);
	}

	private Table runQuery(String query) throws Exception {
		SparqlEndpointInterface sei = TestGraph.getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		if (!res.getSuccess()) {
			throw new Exception("Failure querying: " + query + " \nrationale: "	+ res.getRationaleAsString(" "));
		}
		Table tab = res.getTable();
		return tab;
	}

	private void runDeleteQuery(String query) throws Exception {
		SparqlEndpointInterface sei = TestGraph.getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		if (!res.getSuccess()) {
			throw new Exception("Failure running delete query: \n" + query + "\n rationale: " + res.getRationaleAsString(" "));
		}
	}

	private void deleteUri(String uri) throws Exception {
		String query = String.format(DELETE_URI_FMT, uri, uri);
		SparqlEndpointInterface sei = TestGraph.getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		if (!res.getSuccess()) {
			throw new Exception("Failure deleting uri: " + uri + " rationale: "	+ res.getRationaleAsString(" "));
		}
	}

	private void confirmUriExists(String uri) throws Exception {
		String query = String.format(SELECT_URI_TRIPLES_FMT, uri, uri);
		SparqlEndpointInterface sei = TestGraph.getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		if (!res.getSuccess()) {
			throw new Exception("Failure querying uri: " + uri + " rationale: "	+ res.getRationaleAsString(" "));
		}
		if (res.getTable().getNumRows() < 1) {
			throw new Exception("No triples were found for uri: " + uri);
		}
	}	
	
}
