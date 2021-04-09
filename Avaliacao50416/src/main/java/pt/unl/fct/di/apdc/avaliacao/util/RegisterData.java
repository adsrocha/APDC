package pt.unl.fct.di.apdc.avaliacao.util;

public class RegisterData {

	public String username;
	public String pwd;
	public String pwdConfirm;
	public String email;
	public String perfil;
	public String telFixo;
	public String telemovel;
	public String morada;
	public String moradaComp;
	public String localidade;
	public String codPostal;
	public String tokenId;
	public String deleteUser;
	public String changeUser;
	public String role;
	public String state;
	public String newPwd;
	public String newPwdConfirm;

	private boolean valid;

	public RegisterData() {
	}

	public RegisterData(String username, String email, String pwd, String pwdConfirm, String perfil, String telFixo,
			String telemovel, String morada, String moradaComp, String localidade, String codPostal, String tokenId,
			String deleteUser, String changeUser, String role, String state, String newPwd, String newPwdConfirm) {
		this.username = username;
		this.email = email;
		this.pwd = pwd;
		this.pwdConfirm = pwdConfirm;
		this.telFixo = telFixo;
		this.telemovel = telemovel;
		this.morada = morada;
		this.moradaComp = moradaComp;
		this.localidade = localidade;
		this.codPostal = codPostal;
		this.tokenId = tokenId;
		this.deleteUser = deleteUser;
		this.changeUser = changeUser;
		this.role = role;
		this.state = state;
		this.newPwd = newPwd;
		this.newPwdConfirm = newPwdConfirm;
	}

	public boolean isValidRegistration() {
		valid = true;

		if (!email.contains("@"))
			valid = false;

		if (!email.contains(".com") && !email.contains(".pt"))
			valid = false;

		if (!pwd.equals(pwdConfirm))
			valid = false;

		if (pwd.length() < 8)
			valid = false;

		return valid;
	}
}
