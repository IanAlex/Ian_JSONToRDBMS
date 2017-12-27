# Ian_JSONToRDBMS

This application  reads records from input JSON file (input_data_sampleA.json) and extracts fields ‘name’, ‘description’, 'category' and 'offers_total' and saves the fields in a MySQL table (ija_tp.JsonData) It takes into account that file size can be much bigger than available RAM.   Please note that actual content of JSON file is unformatted, so indentations were not relied upon in parsing it.

From Maven an executable Jar (IanJsonToRdbms) is used to operate on file input_data_sampleA.json to populate the databasename.tablename = ija_tp.TeraPeakJsonData in MySQL database with 100000 records.  ija_tp.JsonData is set up with columns:
(name VARCHAR(10500),description VARCHAR(10500),category VARCHAR(300), offers_total INT)
