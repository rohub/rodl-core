package org.rohub.rodl.oauth;


public class UserInfo {
	
	protected String sub;
	protected String VRC;
	protected String given_name;
	protected String family_name;
	protected String email;
	
	public String getSub() {
		return sub;
	}
	public void setSub(String sub) {
		this.sub = sub;
	}
	public String getVRC() {
		return VRC;
	}
	public void setVRC(String vRC) {
		VRC = vRC;
	}
	public String getGiven_name() {
		return given_name;
	}
	public void setGiven_name(String given_name) {
		this.given_name = given_name;
	}
	public String getFamily_name() {
		return family_name;
	}
	public void setFamily_name(String family_name) {
		this.family_name = family_name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
}
