package web;

public class detailedURL implements Comparable<detailedURL> {
	private int urlid;
	private String url;
	private String description;
	private String title;
	private String image;
	private int rank;
	
	public detailedURL() {
		urlid = -1;
		url = null;
		description = null;	
		title = null;
		image = null;
		rank = -1;
	}
	
	public detailedURL(int urlid, String url, String description, String title, String image, int rank) {
		this.urlid = urlid;
		this.url = url;
		this.description = description;
		this.title = title;
		this.image = image;
		this.rank = rank;
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
	
	public int getRank() {
		return this.rank;
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
	
	public void setRank(int rank) {
		this.rank = rank;
	}

	@Override
	public int compareTo(detailedURL dURL) {
		return this.getRank() - dURL.getRank();
	}
}
