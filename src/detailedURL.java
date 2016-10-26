import java.util.*;

public class detailedURL {
	private int urlid;
	private String url;
	private String description;
	
	detailedURL() {
		urlid = -1;
		url = null;
		description = null;		
	}
	
	detailedURL(int urlid, String url, String description) {
		this.urlid = urlid;
		this.url = url;
		this.description = description;
	}
	
	public int getURLID() {
		return this.urlid;
	}
	
	public String getURL() {
		return this.url;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void setURLID(int urlid) {
		this.urlid = urlid;
	}
	
	public void setURL(String url) {
		this.url = url;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public ArrayList<String> detailsToString() {
		ArrayList<String> tmp = new ArrayList<String>();
		
		tmp.add(String.valueOf(getURLID()));
		tmp.add(getURL());
		tmp.add(getDescription());
		
		return tmp;
	}
}
