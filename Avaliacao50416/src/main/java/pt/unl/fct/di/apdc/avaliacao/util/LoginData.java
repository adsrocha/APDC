package pt.unl.fct.di.apdc.avaliacao.util;

public class LoginData {

	public String username;
	public String pwd;
	public String tokenId;
	

	public LoginData() {

	}

	public LoginData(String username, String password, String tokenId) {
		this.username = username;
		this.pwd = password;
		this.tokenId = tokenId;
	}
}