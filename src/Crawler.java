import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
        stat.executeUpdate("CREATE TABLE URLS (urlid INT, url VARCHAR(512), description VARCHAR(200), image VARCHAR(512), rank INT, title VARCHAR(200))");
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
			PreparedStatement pst = connection.prepareStatement("INSERT INTO URLS (urlid, url, rank) VALUES(?, ?, ?)");
			pst.setInt(1, NextURLID);
			pst.setString(2, url);
			pst.setInt(3, 1);
			pst.executeUpdate();
			pst.close();
			
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
			PreparedStatement pst = connection.prepareStatement("INSERT INTO WORDS VALUES(?, ?)");
			pst.setString(1, word);
			pst.setString(2, urllist);
			pst.executeUpdate();
			pst.close();
		}
	}
	
	public void incrementRank(String url) throws SQLException {
		synchronized(urlTableLock) {
			PreparedStatement pst = connection.prepareStatement("SELECT * FROM URLS WHERE url = ?");
			pst.setString(1, url);
			ResultSet result = pst.executeQuery();
			result.next();
			
			// Get old rank
			int prevRank = result.getInt(5);
			pst.close();
			
			PreparedStatement pst2 = connection.prepareStatement("UPDATE urls SET rank = ? WHERE url = ?");
			pst2.setInt(1, prevRank + 1);
			pst2.setString(2, url);
			pst2.executeUpdate();
			pst2.close();
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
   	
   	public detailedURL getDetails(int urlid) throws SQLException {
   		PreparedStatement pst = connection.prepareStatement("SELECT * FROM urls WHERE urlid = ?");
   		pst.setInt(1, urlid);
   		
   		ResultSet res = pst.executeQuery();
   		if(res.next()) {
   			String url = res.getString(2);
   	   		String description = res.getString(3);
   	   		String image = res.getString(4);
   	   		String title = res.getString(6);
   	   		
   	   		detailedURL detail = new detailedURL(urlid, url, description, title, image);
   	   		
   	   		return detail;
   		}
   		else {
   			return null;
   		}
   	}
   	
   	String getDescription(Document doc) throws IOException {
   		String description;
   		
		description = this.getMetaTag(doc, "description");
   		
   		if (description == null) {
   			description = this.getMetaTag(doc, "og:description");
   		}
   		
   		if (description == null && doc.body().text() != null) {
   			description = doc.body().text();
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
   	
   	String getTitle(Document doc) {
   		String title;
   		
   		title = doc.title();
   		
   		return title;
   	}
   	
   	String getImage(Document doc) {
   		Elements images = doc.select("img");
   		String image = null;
   		
   		for (Element e : images) {
   			image = e.attr("abs:src");
   			break;
   		}
   		
   		return image;
   	}
   	   	
   	public void updateURL(String url, String description, String imageURL, String title) throws SQLException {
   		synchronized(urlTableLock) {
   			PreparedStatement pst = connection.prepareStatement("UPDATE urls SET description = ?, image = ?, title = ? WHERE url = ?");
   			pst.setString(1, description);
   			pst.setString(2, imageURL);
   			pst.setString(3, title);
   			pst.setString(4, url);
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
   	
   	public String parseURL(String url) {
   		String parsedURL = url.replaceAll("/index.html", "");
   		parsedURL = parsedURL.replaceAll("/[a-zA-Z_0-9]+.php", "");
   		parsedURL = parsedURL.replaceAll("#[a-zA-Z_0-9]*", "");
   		
  		int len = parsedURL.length();
  		
		if (len > 0 && parsedURL.charAt(len - 1) == '/') {
			parsedURL = parsedURL.substring(0, len - 1);
		}
		
		return parsedURL;
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
   			String description = getDescription(doc);   			
   			String imageURL = getImage(doc);
   			String title = getTitle(doc);
   			
   			// Update description and image for URL
   			updateURL(url, description, imageURL, title);
   			
   			Elements elems = doc.select("a");
   	   		
   	   		for (Element e : elems) {   			
   	   			String absURL = e.attr("abs:href");
   	   			String parsedURL = parseURL(absURL);
   	   			
   	   			// Check if valid
   	   			if (validURL(parsedURL)) {   				
   	   				if (urlInDB(parsedURL)) {
   	   					incrementRank(parsedURL);
   	   				}
   	   				else {
   	   					// Add if nextURLID < maxURLS
   	   					if (NextURLID < maxURLS) {
   	   						this.insertURLInDB(parsedURL);
   	   					}
   	   				}
   	   			}
   	   		}
   		}
   		
   		// Analyze words and add to database
   		analyzeWords(doc);
   		
   		// Update properties
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
   			NextURLIDScanned = 0;
   			NextURLID = 0;
   			
   			for (String root : roots) {
   				String parsedRoot = parseURL(root);
   				this.insertURLInDB(parsedRoot);
   			}
   			
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

