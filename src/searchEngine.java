import java.io.IOException;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

import web.detailedURL;

/**
 * Servlet implementation class searchEngine
 */
@WebServlet(description = "Search Engine", urlPatterns = {"/searchEngine"})
public class searchEngine extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Properties props;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		readProperties();
		
		try {
			String query = request.getParameter("query");
			String nextJSP = "/WEB-INF/searchResults.jsp";
			
			// Parse query
			query = query.toLowerCase();
			String[] searchWords = query.split("\\s+");
			
			// Create a crawler
			Crawler crawl = new Crawler(props);
			
			// Store URL IDs
			ArrayList<ArrayList<Integer>> ids = new ArrayList<ArrayList<Integer>>();
			
			// Get associated URLS to each word
			for (String word : searchWords) {
				ArrayList<Integer> urls = new ArrayList<Integer>();
				
				if (crawl.wordInDB(word)) {
					String tmpString = crawl.getWordList(word).trim();
					String[] tmpURLS = tmpString.split(" ");
					
					for (int i = 0; i < tmpURLS.length; i++) {
						if (!tmpURLS[i].equals(",")) {
							urls.add(Integer.parseInt(tmpURLS[i]));
						}
					}
				}
				
				if (!urls.isEmpty()) {
					ids.add(urls);
				}
			}
			
			if (ids.isEmpty()) {
				// Empty Search
				request.setAttribute("query", query);
				request.setAttribute("searchResults", null);
				
				RequestDispatcher dispatch = this.getServletContext().getRequestDispatcher(nextJSP);
				dispatch.forward(request, response);
			}
			else {
				ArrayList<Integer> commonURLS = new ArrayList<Integer>();
				commonURLS.addAll(ids.get(0));
				
				for (ArrayList<Integer> list : ids) {
					commonURLS.retainAll(list);
				}
				
				if (!commonURLS.isEmpty()) {
					ArrayList<detailedURL> searchResults = new ArrayList<detailedURL>();
					
					for (int id : commonURLS) {
						detailedURL detail = crawl.getDetails(id);
						
						if (detail != null) {
							searchResults.add(detail);
						}
					}
					
					Collections.sort(searchResults);
					
					request.setAttribute("query", query);
					request.setAttribute("searchResults", searchResults);
					
					RequestDispatcher dispatch = this.getServletContext().getRequestDispatcher(nextJSP);
					dispatch.forward(request, response);
				}
				else {
					// Empty Search
					request.setAttribute("query", query);
					request.setAttribute("searchResults", null);
					
					RequestDispatcher dispatch = this.getServletContext().getRequestDispatcher(nextJSP);
					dispatch.forward(request, response);
				}
			}
		}
		catch (Exception e) {
			//e.printStackTrace();
			
			RequestDispatcher dispatch = this.getServletContext().getRequestDispatcher("/WEB-INF/searchResults.jsp");
			dispatch.forward(request, response);
		}
	}

	// Read from properties file
	public void readProperties() throws IOException {
		props = new Properties();
		props.load(getServletContext().getResourceAsStream("/WEB-INF/database.properties"));
	}
}
