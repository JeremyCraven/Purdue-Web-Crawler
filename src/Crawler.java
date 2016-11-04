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

import org.apache.commons.validator.routines.UrlValidator;
import web.detailedURL;

public class Crawler implements Runnable {
	private final Object varLock = new Object();
	private final Object wordLock = new Object();
	private final Object urlTableLock = new Object();
	private final Object wordTableLock = new Object();
	private final static int numThreads = 5;
	Connection connection;
	
	int NextURLID, NextURLIDScanned, maxURLS;
	public Properties props;
	
	String domain;

	// Constructor
	Crawler() {
		this.initCrawl();
	}
	
	Crawler(Properties p) {
		try {
			this.props = p;
			
			openConnection();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Start a thread
	public void run() {
		try {
			this.crawl();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Read from properties file
	public void readProperties() throws IOException {
		synchronized(varLock) {
			props = new Properties();
	      	FileInputStream in = new FileInputStream("WebContent/WEB-INF/database.properties");
	      	props.load(in);
	      	in.close();
		}
	}
	
	public void updateProperties() throws IOException {
		synchronized(varLock) {
			FileOutputStream out = new FileOutputStream("WebContent/WEB-INF/database.properties");
			
			props.setProperty("crawler.NextURLID", NextURLID + "");
			props.setProperty("crawler.NextURLIDScanned", NextURLIDScanned + "");
			
			props.store(out, null);
			
			out.close();
		}
	}
	
	public void updateVariables() {
		synchronized(varLock) {
			NextURLID = Integer.parseInt(props.getProperty("crawler.NextURLID"));
		    NextURLIDScanned = Integer.parseInt(props.getProperty("crawler.NextURLIDScanned"));
		}
	}

	// Open the database connection
	public void openConnection() throws SQLException, IOException {
		String drivers = props.getProperty("jdbc.drivers");
      	if (drivers != null) System.setProperty("jdbc.drivers", drivers);

      	String url = props.getProperty("jdbc.url");
      	String username = props.getProperty("jdbc.username");
      	String password = props.getProperty("jdbc.password");

		connection = DriverManager.getConnection(url, username, password);
   	}

	// Create the tables
	public void createDB() throws SQLException, IOException {
		openConnection();

        Statement stat = connection.createStatement();
		
		// Delete the table first if any
		try {
			stat.executeUpdate("DROP TABLE URLS");
			stat.executeUpdate("DROP TABLE WORDS");
		}
		catch (Exception e) {
		}
			
		// Create the table
        stat.executeUpdate("CREATE TABLE URLS (urlid INT, url VARCHAR(512), description VARCHAR(200))");
        stat.executeUpdate("CREATE TABLE WORDS (word VARCHAR(2048), urllist VARCHAR(16384))");
        stat.close();
	}

	public synchronized boolean urlInDB(String urlFound) throws SQLException, IOException {
		synchronized(urlTableLock) {
			Statement stat = connection.createStatement();
			ResultSet result = stat.executeQuery("SELECT * FROM urls WHERE url LIKE '" + urlFound + "'");
			
			if (result.next()) {
				stat.close();
				return true;
			}
			else {
				stat.close();
				return false;
			}
		}
	}

	public void insertURLInDB(String url) throws SQLException, IOException {
		synchronized(urlTableLock) {
			Statement stat = connection.createStatement();
			String query = "INSERT INTO URLS VALUES ('" + NextURLID + "','" + url + "','')";
			stat.executeUpdate(query);
			stat.close();
			
			NextURLID++;
			updateProperties();
		}
	}
	
	public boolean wordInDB(String word) throws SQLException, IOException {
		synchronized(wordTableLock) {
			Statement stat = connection.createStatement();
			ResultSet result = stat.executeQuery("SELECT * FROM words WHERE word LIKE '" + word + "'");
			
			if (result.next()) {
				stat.close();
				return true;
			}
			else {
				stat.close();
				return false;
			}
		}
	}
	
	public void insertWordInDB(String word, String urllist) throws SQLException, IOException {
		synchronized(wordTableLock) {
			Statement stat = connection.createStatement();
			String query = "INSERT INTO WORDS VALUES ('" + word + "','" + urllist + "')";
			stat.executeUpdate(query);
			stat.close();
		}
	}
	
	public String getWordList(String word) throws SQLException {
		synchronized(wordTableLock) {
			PreparedStatement pst = connection.prepareStatement("SELECT * FROM WORDS WHERE word = ?");
			pst.setString(1, word);
			
			// Get the word entry in the DB
			ResultSet result = pst.executeQuery();
			result.next();
			
			// Extract the list of associated URLS
			String urlList = result.getString(2);
			
			return urlList;
		}	
	}
   	
   	// Get the next URL from the URLS table
   	public synchronized String getNextURL() {
   		this.updateVariables();
   		
   		Statement stat;
		ResultSet result;
		
		try {
			stat = connection.createStatement();
   			result = stat.executeQuery("SELECT * FROM urls WHERE urlid = " + this.NextURLIDScanned);
   			String url = null;
   			
   			boolean exists = result.next();
   			
   			if (exists) {
   				url = result.getString(2);
   				NextURLIDScanned++;
   				this.updateProperties();
   			}
   			
   			stat.close();
   			
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
   		Document doc = null;
   		String description;
   		
   		try {
   			if (validURL(url)) {
   				doc = Jsoup.connect(url).get();
   			}
   		}
   		catch (Exception e) {
   			e.printStackTrace();
   		}
   		
   		if (doc != null) {
   			description = this.getMetaTag(doc, "description");
   	   		
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
   		}
   		else {
   			description = "NO DESCRIPTION";
   		}
   		
   		
   		return description;
   	}
   	
   	public detailedURL getDetails(int urlid) throws SQLException {
   		PreparedStatement pst = connection.prepareStatement("SELECT * FROM urls WHERE urlid = ?");
   		pst.setInt(1, urlid);
   		
   		ResultSet res = pst.executeQuery();
   		if(res.next()) {
   			String url = res.getString(2);
   	   		String description = res.getString(3);
   	   		
   	   		detailedURL detail = new detailedURL(urlid, url, description);
   	   		
   	   		return detail;
   		}
   		else {
   			return null;
   		}
   	}
   	
   	public void updateDescription(String url, String description) throws SQLException {
   		synchronized(urlTableLock) {
   			PreparedStatement pst = connection.prepareStatement("UPDATE urls SET description = ? WHERE url = ?");
   			pst.setString(1, description);
   			pst.setString(2, url);
   			pst.executeUpdate();
   			pst.close();
   		}
   	}
   	
   	boolean validURL(String url) {
		if (url.contains(domain)) {
			// Check header for content type
			try {
				URL testHTML = new URL(url);
   				HttpURLConnection urlc = (HttpURLConnection)testHTML.openConnection();
   				
   				urlc.setAllowUserInteraction(false);
   				urlc.setDoInput(true);
   				urlc.setDoOutput(false);
   				urlc.setUseCaches(true);
   				urlc.setRequestMethod("HEAD");
   				urlc.connect();
   				String mime = urlc.getContentType();
   				
   				if (mime.contains("text/html")) {
   					return true;
   				}
   				else {
   					return false;
   				}
			}
			catch (Exception e) {
				return false;
			}
		}
		else {
			return false;
		}
   	}
   	
   	void fetchURLS(String url) throws IOException, SQLException {
   		System.out.println("Processing: " + url);
   		
   		Document doc = null;
   		
   		try {
   			if (validURL(url)) {
   				doc = Jsoup.connect(url).get();
   			}
   		}
   		catch (Exception e) {
   			e.printStackTrace();
   		}
   		
   		if (doc != null) {
   			Elements elems = doc.select("a");
   	   		
   	   		for (Element e : elems) {   			
   	   			String absURL = e.attr("abs:href");
   	   			
   	   			// Check if valid
   	   			if (validURL(absURL)) {   				
   	   				if (urlInDB(absURL)) {
   	   					// TODO: Increment rank
   	   				}
   	   				else {
   	   					// Add if nextURLID < maxURLS
   	   					if (NextURLID < maxURLS) {
   	   						this.insertURLInDB(absURL);
   	   					}
   	   				}
   	   			}
   	   			else {
   	   				System.out.println("Invalid URL: " + absURL);
   	   			}
   	   		}
   		}
   		
   		// Analyze words and add to database
   		analyzeWords(doc);
   		
   		// Update properties
   		NextURLIDScanned++;
   		updateProperties();
   	}
   	
   	public void analyzeWords(Document doc) throws IOException, SQLException {  
   		if (doc != null) {
   		// Get text of the document
   			String text = doc.text();
   			
   			// Only keep alphanumeric characters
   			text = text.replaceAll("[^A-Za-z0-9 ]", "");
   			
   			// Convert all to lower case
   			text = text.toLowerCase();
   			
   			// Split on all space characters
   			String[] words = text.split("\\s+");
   			
   			System.out.println("Words Count: " + words.length);
   			
   			// Process each word
   			for (String word : words) {
   				int urlIndex = NextURLIDScanned - 1;
   				
   				if (wordInDB(word)) {
   					String urlList = getWordList(word);
   					
   					// Add the urlIndex to the word if not already there
   					if (!urlList.contains(" " + urlIndex + " ")) {
   						urlList += (", " + urlIndex + " ");
   						
   						synchronized(wordLock) {
   							PreparedStatement pst = connection.prepareStatement("UPDATE WORDS SET urllist = ? WHERE word = ?");
   							pst.setString(1, urlList);
   							pst.setString(2, word);
   							pst.executeUpdate();
   							pst.close();
   						}
   					}
   				}
   				else {
   					this.insertWordInDB(word, " " + urlIndex + " ");
   				}
   			}
   		}
   	}
   	
   	void initCrawl() {   		
   		try {
   			this.readProperties();
   			this.createDB();
   			
   			String[] roots = this.props.getProperty("crawler.roots").split(",");
   			maxURLS = Integer.parseInt(this.props.getProperty("crawler.maxurls"));
   			domain = this.props.getProperty("crawler.domain");
   			
   			for (String root : roots) {
   				this.insertURLInDB(root);
   			}
   			
   			this.updateVariables();
   			this.updateProperties();
   		}
   		catch (Exception e) {
   			e.printStackTrace();
   		}
   	}
   	
   	public void crawl() throws IOException, SQLException, InterruptedException {   		
   		this.updateVariables();
   		
   		while (NextURLIDScanned < NextURLID) {
   			String url = this.getNextURL();
   			
   			if (url != null) {
   				String description = this.getDescription(url);
   	   			
   	   			// Add description to the table
   	   			updateDescription(url, description);
   	   			
   	   			// Get URLS
   	   			this.fetchURLS(url); 
   			}	 			
   		}
   	}

   	public static void main(String[] args) {
   		long start = System.currentTimeMillis();
   		
		Crawler crawler = new Crawler();
		
		boolean concurrent = false;
		
		if (concurrent) {
			Thread threads[] = new Thread[numThreads];
			
			for (int i = 0; i < threads.length; i++) {
				threads[i] = new Thread(crawler, "thread" + i);
			}
			
			for (int i = 0; i < threads.length; i++) {
				threads[i].start();
			}
			
			try {
				for (int i = 0; i < threads.length; i++) {
					threads[i].join();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				crawler.crawl();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Crawling complete.");
		
		long end = System.currentTimeMillis();
		System.out.println("Execution Time: " + (end - start));
    }
}

