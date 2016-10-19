import java.io.*;
import java.net.*;
import java.util.regex.*;
import javax.servlet.annotation.WebServlet;
import java.sql.*;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler {
	Connection connection;
	int urlID, NextURLID, NextURLIDScanned;
	public Properties props;

	Crawler() {
		urlID = 0;
		NextURLID = 0;
		NextURLIDScanned = 0;
	}

	public void readProperties() throws IOException {
      	props = new Properties();
      	FileInputStream in = new FileInputStream("database.properties");
      	props.load(in);
      	in.close();
	}

	public void openConnection() throws SQLException, IOException {
		String drivers = props.getProperty("jdbc.drivers");
      	if (drivers != null) System.setProperty("jdbc.drivers", drivers);

      	String url = props.getProperty("jdbc.url");
      	String username = props.getProperty("jdbc.username");
      	String password = props.getProperty("jdbc.password");

		connection = DriverManager.getConnection(url, username, password);
   	}

	public void createDB() throws SQLException, IOException {
		openConnection();

        Statement stat = connection.createStatement();
		
		// Delete the table first if any
		try {
			stat.executeUpdate("DROP TABLE URLS");
		}
		catch (Exception e) {
		}
			
		// Create the table
        stat.executeUpdate("CREATE TABLE URLS (urlid INT, url VARCHAR(512), description VARCHAR(200))");
	}

	public boolean urlInDB(String urlFound) throws SQLException, IOException {
        Statement stat = connection.createStatement();
		ResultSet result = stat.executeQuery("SELECT * FROM urls WHERE url LIKE '"+urlFound+"'");

		if (result.next()) {
	        	System.out.println("URL " + urlFound + " already in DB.");
			return true;
		}
	    
		return false;
	}

	public void insertURLInDB(String url) throws SQLException, IOException {
        Statement stat = connection.createStatement();
		String query = "INSERT INTO urls VALUES ('" + urlID + "','" + url + "','')";
		stat.executeUpdate(query);
		urlID++;
	}

   	public void fetchURL(String urlScanned) {
		try {
			URL url = new URL(urlScanned);
			System.out.println("urlscanned=" + urlScanned + " url.path=" + url.getPath());
 
    		// open reader for URL
    		InputStreamReader in = new InputStreamReader(url.openStream());

    		// read contents into string builder
    		StringBuilder input = new StringBuilder();
    		int ch;
			
    		while ((ch = in.read()) != -1) {
         			input.append((char) ch);
			}

     		// search for all occurrences of pattern
    		String patternString = "<a\\s+href\\s*=\\s*(\"[^\"]*\"|[^\\s>]*)\\s*>";
    		Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    		Matcher matcher = pattern.matcher(input);
		
			while (matcher.find()) {
    			int start = matcher.start();
    			int end = matcher.end();
    			String match = input.substring(start, end);
				String urlFound = matcher.group(1);
				System.out.println(urlFound);

				// Check if it is already in the database
				if (!urlInDB(urlFound)) {
					insertURLInDB(urlFound);
				}				
	
    			System.out.println(match);
 			}
		}
      	catch (Exception e)
      	{
       		e.printStackTrace();
      	}
	}
   	
   	public String getNextURL() {
   		Statement stat;
		ResultSet result;
		
		try {
			stat = connection.createStatement();
   			result = stat.executeQuery("SELECT * FROM urls WHERE urlid = " + this.NextURLIDScanned);
		
   			result.next();
   			String url = result.getString(2);
   			stat.close();
   			
   			NextURLIDScanned++;
   			
   			return url;
		}
		catch (Exception e) {
			e.printStackTrace();
			
			return null;
		}
   	}
   	
   	// Extract the document description
   	String getMetaTag(Document doc, String attr) {
   		if (doc == null)  {
   			return null;
   		}
   		
   		Elements elements = doc.select("meta[name=" + attr + "]");
   		
   		for (Element e : elements) {
   			String s = e.attr("content");
   			
   			if (s != null) {
   				return s;
   			}
   		}
   		
   		elements = doc.select("meta[property=" + attr + "]");
   		
   		for (Element e : elements) {
   			String s = e.attr("content");
   			
   			if (s != null) {
   				return s;
   			}
   		}
   		
   		return null;
   	}
   	
   	String getDescription(String url) throws IOException {   		
   		Document doc = Jsoup.connect(url).get();
   		
   		String description = this.getMetaTag(doc, "description");
   		
   		if (description == null) {
   			description = this.getMetaTag(doc, "og:description");
   		}
   		
   		if (description == null) {
   			description = doc.text();
   		}
   		
   		if (description != null && description.length() > 100) {
   			description = description.substring(0, 100);
   		}
   		else if (description == null) {
   			description = "NO DESCRIPTION";
   		}
   		
   		return description;
   	}
   	
   	void fetchURLS(String url) throws IOException {
   		Document doc = Jsoup.connect(url).get();
   		Elements elems = doc.select("a");
   		
   		for (Element e : elems) {   			
   			String absURL = e.attr("abs:href");
   			
   			// Check if url in DB, if yes increment rank, if not add
   		}
   	}
   	
   	void initCrawl() {   		
   		try {
   			this.readProperties();
   			this.createDB();
   			
   			String[] roots = this.props.getProperty("crawler.roots").split(",");
   			
   			for (String root : roots) {
   				this.insertURLInDB(root);
   				NextURLID++;
   			}
   			
   			this.crawl();
   		}
   		catch (Exception e) {
   			e.printStackTrace();
   		}
   	}
   	
   	void crawl() throws IOException {
   		while (NextURLIDScanned < NextURLID) {   			
   			String url = this.getNextURL();
   			String description = this.getDescription(url);
   			
   			// TODO: Add to description to the table
   			System.out.println(description);
   			
   			// Get urls
   			this.fetchURLS(url);
   			
   			//System.out.println(url);   			
   		}
   	}

   	public static void main(String[] args) {
		Crawler crawler = new Crawler();
		crawler.initCrawl();
    }
}

