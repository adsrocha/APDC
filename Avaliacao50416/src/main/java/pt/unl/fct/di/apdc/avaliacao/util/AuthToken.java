package pt.unl.fct.di.apdc.avaliacao.util;

import java.util.UUID;

import pt.unl.fct.di.apdc.avaliacao.util.AuthToken;

public class AuthToken {
	public static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2; // 2h
	public String username;
	public String tokenID;
	public String role;
	public long creationData;
	public long expirationData;

	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.tokenID = UUID.randomUUID().toString();
		this.creationData = System.currentTimeMillis();
		this.expirationData = this.creationData + AuthToken.EXPIRATION_TIME;
	}

	public void invalidateToken() {
		expirationData = System.currentTimeMillis();
	}
}
