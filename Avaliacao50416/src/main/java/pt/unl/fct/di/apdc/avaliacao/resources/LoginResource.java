package pt.unl.fct.di.apdc.avaliacao.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.avaliacao.resources.LoginResource;
import pt.unl.fct.di.apdc.avaliacao.util.AuthToken;
import pt.unl.fct.di.apdc.avaliacao.util.LoginData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");
	AuthToken token;

	private final Gson g = new Gson();

	public LoginResource() {
	}

	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data) {
		// FAZER TXN
		LOG.fine("Attempt to login user: " + data.username);

		Key userKey = userKeyFactory.newKey(data.username);
		Key tokenKey = tokenKeyFactory.newKey(data.username);
		Entity user = datastore.get(userKey);
		Entity tok = datastore.get(tokenKey);

		if (user != null) {

			if (user.getString("account_state").equals("ENABLED")) {
				
				String hashedPwd = user.getString("user_pwd");

				if (hashedPwd.equals(DigestUtils.sha512Hex(data.pwd))) {
					token = new AuthToken(data.username, user.getString("user_role"));

					tok = Entity.newBuilder(tokenKey).set("user_name", data.username).set("tokenId", token.tokenID)
							.set("role", token.role).set("creation_data", token.creationData)
							.set("expirationData", token.expirationData).build();

					datastore.put(tok);

					return Response.ok(g.toJson(token)).build();
				} else {
					LOG.warning("Wrong password for username: " + data.username);
					return Response.status(Status.FORBIDDEN).entity("Wrong password or username").build();
				}
			} else {
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).entity("This account has been disabled.").build();
			}
		} else {
			LOG.warning("Failed login attempt for username: " + data.username);
			return Response.status(Status.FORBIDDEN).entity("Wrong password or username").build();
		}

	}

	@PUT
	@Path("/logout")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogout(LoginData data) {

		LOG.fine("Attempt to logout user: " + data.username);

		Key userKey = userKeyFactory.newKey(data.username);
		Key tokenKey = tokenKeyFactory.newKey(data.username);
		Entity user = datastore.get(userKey);
		Entity tok = datastore.get(tokenKey);

		if (user == null) {
			return Response.status(Status.BAD_REQUEST).entity("User doesn't exists.").build();
		} else {
			if (data.tokenId.equals(tok.getString("tokenId"))
					&& (tok.getLong("expirationData") > System.currentTimeMillis())) {

				tok = Entity.newBuilder(tokenKey).set("user_name", tok.getString("user_name"))
						.set("tokenId", tok.getString("tokenId")).set("creation_data", tok.getLong("creation_data"))
						.set("role", tok.getString("role")).set("expirationData", System.currentTimeMillis()).build();

				datastore.update(tok);

				return Response.ok().build();
			} else {
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong.").build();
			}

		}
	}
}
