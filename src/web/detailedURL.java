package web;

public class detailedURL {
	private int urlid;
	private String url;
	private String description;
	private String title;
	private String image;
	
	public detailedURL() {
		urlid = -1;
		url = null;
		description = null;		
	}
	
	public detailedURL(int urlid, String url, String description, String title, String image) {
		this.urlid = urlid;
		this.url = url;
		this.description = description;
		this.title = title;
		this.image = image;
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
	
	public String getTitle() {
		return this.title;
	}
	
	public String getImage() {
		return this.image;
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
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setImage(String image) {
		this.image = image;
	}
	
	/*public ArrayList<String> detailsToString() {
		ArrayList<String> tmp = new ArrayList<String>();
		
		tmp.add(String.valueOf(getURLID()));
		tmp.add(getURL());
		tmp.add(getDescription());
		
		return tmp;
	}*/
}
