package com.jsontordbms.ija;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jackson.map.*;

import java.io.*;
import java.sql.*;
import java.util.*;

public class JsonToDatabase {
	
	private static final String dbURL = "jdbc:mysql://localhost";  
	private static final String dbUser = "root";  
	private static final String dbPassword = "ija-tp";  
	private static final String DBTABLE = "ija_tp.JsonData";  
	private static final int NUM_JSONRECS_FOR_TABLE = 100000;  
	private static final int DBTABLE_INSERT_BATCHSIZE = 10000;  
	private static final int OFFERS_TOTAL_NEVER = -99999; // assume that this will never be value for offers_total in JSON
	
	
	private static Connection conn = null;
	private static List<JsonRecord> jsonRecordList = new ArrayList<JsonRecord>();
	
	public static void main(String[] args) {
		establishMySQL();
		putJsonFileToList();
		writeListToDatabase();
		System.exit(0);
	}

	/*
	 * Set up the database connection and create the table (DBTABLE) to write the JSON data
	 */	
	private static void establishMySQL() {
		//StringBuilder objects for DDL
		StringBuilder createTableSB = new StringBuilder();
		createTableSB.append("CREATE TABLE ");
		createTableSB.append(DBTABLE);
		createTableSB.append(" (name VARCHAR(10500),description VARCHAR(10500),category VARCHAR(300), offers_total INT) ");
		StringBuilder dropTableSB = new StringBuilder();
		dropTableSB.append("DROP TABLE ");
		dropTableSB.append(DBTABLE);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(dbURL,dbUser,dbPassword);
			conn.setAutoCommit(true);
			try {
				conn.createStatement().execute(createTableSB.toString());
			} catch(Exception ex) {
				conn.createStatement().execute(dropTableSB.toString());
				conn.createStatement().execute(createTableSB.toString());
			}
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *  Parse the JSON file using Jackson Stream to populate JsonRecord objects which we put in the jsonRecordList
	 *  The iterations continue until either we have reached the end of the file or the size of the jsonRecordList
	 *  has reached NUM_JSONRECS_FOR_TABLE
	 */	
	private static void putJsonFileToList() {
		
		JsonParser jParser = null;	
		
		try {
			
			JsonFactory jfactory = new JsonFactory();
			
			jParser = jfactory.createJsonParser(new File("input_data_sampleA.json"));
			
			JsonToken jToken;			
			
			String name = null;
			String description = null;
			String category = null;
			int offersTotal = OFFERS_TOTAL_NEVER;  // assume that this will never be offers_total value in the JSON file
			
			while ((jToken = jParser.nextToken()) != null && jToken != JsonToken.NOT_AVAILABLE && jsonRecordList.size() < NUM_JSONRECS_FOR_TABLE) {
				
				String fieldname = jParser.getCurrentName();
				
				if ("name".equals(fieldname)) {
					// get value corresponding to name
					jParser.nextToken();
					name = jParser.getText().trim(); 
					// if all record fields are populated then add a new JsonRecord to list and reset 
					// name,description,category,offersTotal to 'empty' defaults
					if (name != null && description != null && category != null && offersTotal != OFFERS_TOTAL_NEVER) {
						jsonRecordList.add(new JsonRecord(name, description, category, offersTotal));
						name = null;
						description = null;
						category = null;
						offersTotal = OFFERS_TOTAL_NEVER;
					}
				}
				else if ("description".equals(fieldname)) {
					// get value corresponding to description
					jParser.nextToken();
					description = jParser.getText().trim(); 
					// if all record fields are populated then add a new JsonRecord to list and reset 
					// name,description,category,offersTotal to 'empty' defaults
					if (name != null && description != null && category != null && offersTotal != OFFERS_TOTAL_NEVER) {
						jsonRecordList.add(new JsonRecord(name, description, category, offersTotal));
						name = null;
						description = null;
						category = null;
						offersTotal = OFFERS_TOTAL_NEVER;
					}					
				}
				else if ("category".equals(fieldname)) {
					// get value corresponding to category
					jParser.nextToken();
					category = jParser.getText().trim(); 
					// if all record fields are populated then add a new JsonRecord to list and reset 
					// name,description,category,offersTotal to 'empty' defaults
					if (name != null && description != null && category != null && offersTotal != OFFERS_TOTAL_NEVER) {
						jsonRecordList.add(new JsonRecord(name, description, category, offersTotal));
						name = null;
						description = null;
						category = null;
						offersTotal = OFFERS_TOTAL_NEVER;
					}					
				}
				else if ("offers_total".equals(fieldname)) {
					// get value corresponding to offers_total
					jParser.nextToken();
					offersTotal = jParser.getIntValue(); 
					// if all record fields are populated then add a new JsonRecord to list and reset 
					// name,description,category,offersTotals to 'empty' defaults
					if (name != null && description != null && category != null && offersTotal != OFFERS_TOTAL_NEVER) {
						jsonRecordList.add(new JsonRecord(name, description, category, offersTotal));
						name = null;
						description = null;
						category = null;
						offersTotal = OFFERS_TOTAL_NEVER;
					}					
				}	
				
			}		
			
		}catch (JsonGenerationException e) {
			 
		  	e.printStackTrace();
		 
		} catch (JsonMappingException e) {
		 
			e.printStackTrace();
		 
		} catch (IOException e) {
		 
			e.printStackTrace();
		}
		finally {
			try {
				// close IO resource after iterative processing
				if (jParser != null) {
					jParser.close();
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/*
	 * For each JsonRecord object in the jsonRecordList use the fields in JsonRecord to insert into
	 * DBTABLE (create new table record).   Bulk inserts are utilized. Programming Assignment Part 1 specifies "Run the
	 *  extraction code enough times that there are at least 100,000 rows in the database." so during the insertion loop
	 *  we repeatedly traverse through the jsonRecordList (see below)
	 */
	private static void writeListToDatabase() {
		if (conn != null && !jsonRecordList.isEmpty()) {
			PreparedStatement pStmt = null;
			// prepare the StringBuilder with the INSERT DML for the preparedstatement
			StringBuilder insertTableSB = new StringBuilder();
			insertTableSB.append(" INSERT INTO ");
			insertTableSB.append(DBTABLE);
			insertTableSB.append(" (name, description, category, offers_total) ");
			insertTableSB.append(" VALUES(?,?,?,?) ");
			try {
				pStmt = conn.prepareStatement(insertTableSB.toString());
				int batchCnt = 0;
				int jsonListCnt = 0;
				// loop until we reach NUM_JSONRECS_FOR_TABLE iterations.  Within the loop if we have reached the end of the jsonRecordList
				//  records (jsonListCnt = jsonRecordList.size() -1) then we start again with the jsonRecordList (ie. continue filling the
				// insert batch buffer by repeat of traversing of the list).
				for (int i = 0; i < NUM_JSONRECS_FOR_TABLE; i++) {
					JsonRecord jRec = jsonRecordList.get(jsonListCnt);
					pStmt.setString(1, jRec.getName());
					pStmt.setString(2, jRec.getDescription());
					pStmt.setString(3, jRec.getCategory());
					pStmt.setInt(4, jRec.getOffersTotal());
					pStmt.addBatch();
					batchCnt++;
					// do a batch insert at DBTABLE_INSERT_BATCHSIZE records in pStmt batch buffer
					if ((batchCnt % DBTABLE_INSERT_BATCHSIZE) == 0) {
						pStmt.executeBatch();
 						pStmt.clearBatch();
 						System.out.println("doing batch insert at " + batchCnt + " records.");
						batchCnt = 0;
					}
					// when we reach end of the list, repeat traversing it.
					if(jsonListCnt < (jsonRecordList.size()-1))  {
						jsonListCnt++;
					}
					else {
						jsonListCnt = 0;
					}
				}
				// close PreparedStatement resource after iterative processing
				if (pStmt != null) {
					if (batchCnt >0) {
						pStmt.executeBatch();
					}
					pStmt.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				// when we are finished with database processing, close the connection resource
				try {
					if (conn != null) {
						conn.close();
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	

}
