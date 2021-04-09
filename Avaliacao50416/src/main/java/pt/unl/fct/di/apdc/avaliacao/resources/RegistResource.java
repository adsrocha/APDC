package pt.unl.fct.di.apdc.avaliacao.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.avaliacao.util.RegisterData;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegistResource {

	private static final Logger LOG = Logger.getLogger(RegistResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");
	private final Gson g = new Gson();

	public RegistResource() {
	}

	@POST
	@Path("/regist")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registUser(RegisterData data) {

		LOG.fine("Attempt to register user: " + data.username);

		if (!data.isValidRegistration())
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();

		Transaction txn = datastore.newTransaction();

		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);

			if (user != null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong.").build();
			} else {
				user = Entity.newBuilder(userKey).set("user_name", data.username)
						.set("user_pwd", DigestUtils.sha512Hex(data.pwd)).set("user_email", data.email)
						.set("user_profile", "PRIVADO").set("account_state", "ENABLED").set("user_role", "USER")
						.set("user_phone_number", data.telemovel).set("user_housephone_number", data.telFixo)
						.set("user_address", data.morada).set("user_address_comp", data.moradaComp)
						.set("user_district", data.localidade).set("user_cod_postal", data.codPostal).build();
				txn.add(user);
				LOG.info("User registered " + data.username);
				txn.commit();
				return Response.ok("{}").build();
			}
		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@PUT
	@Path("/remove")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response removeUser(RegisterData data) {

		LOG.fine("Attempt to delete user: " + data.username);

		Transaction txn = datastore.newTransaction();

		try {
			Key userKey = userKeyFactory.newKey(data.username);
			Key deleteUserKey = userKeyFactory.newKey(data.deleteUser);
			Key tokenKey = tokenKeyFactory.newKey(data.username);
			Entity user = txn.get(userKey);
			Entity deleteUser = txn.get(deleteUserKey);
			Entity tok = txn.get(tokenKey);

			if (user == null || deleteUser == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong.").build();
			} else {

				if (tok.getString("tokenId").equals(data.tokenId)
						&& (tok.getLong("expirationData") > System.currentTimeMillis())) {

					if (user.getString("user_role").equals("USER") && data.username.equals(data.deleteUser)) {

						user = Entity.newBuilder(userKey).set("user_name", data.username)
								.set("user_pwd", user.getString("user_pwd"))
								.set("user_email", user.getString("user_email"))
								.set("user_profile", user.getString("user_profile")).set("account_state", "DISABLED")
								.set("user_role", user.getString("user_role"))
								.set("user_phone_number", user.getString("user_phone_number"))
								.set("user_housephone_number", user.getString("user_housephone_number"))
								.set("user_address", user.getString("user_address"))
								.set("user_address_comp", user.getString("user_address_comp"))
								.set("user_district", user.getString("user_district"))
								.set("user_cod_postal", user.getString("user_cod_postal")).build();

						txn.update(user);

						LOG.info("User removed " + data.username);
						txn.commit();
						return Response.ok("Removeu").build();

					} else if (deleteUser.getString("user_role").equals("USER")
							&& (user.getString("user_role").equals("GBO") || user.getString("user_role").equals("GA")
									|| user.getString("user_role").equals("SU"))) {

						deleteUser = Entity.newBuilder(deleteUserKey).set("user_name", data.deleteUser)
								.set("user_pwd", deleteUser.getString("user_pwd"))
								.set("user_email", deleteUser.getString("user_email"))
								.set("user_profile", deleteUser.getString("user_profile"))
								.set("account_state", "DISABLED").set("user_role", deleteUser.getString("user_role"))
								.set("user_phone_number", deleteUser.getString("user_phone_number"))
								.set("user_housephone_number", deleteUser.getString("user_housephone_number"))
								.set("user_address", deleteUser.getString("user_address"))
								.set("user_address_comp", deleteUser.getString("user_address_comp"))
								.set("user_district", deleteUser.getString("user_district"))
								.set("user_cod_postal", deleteUser.getString("user_cod_postal")).build();

						txn.update(deleteUser);

						LOG.info("User removed " + data.deleteUser);
						txn.commit();
						return Response.ok("Removeu").build();

					} else if (deleteUser.getString("user_role").equals("GBO")
							&& (user.getString("user_role").equals("GA") || user.getString("user_role").equals("SU"))) {

						deleteUser = Entity.newBuilder(deleteUserKey).set("user_name", data.deleteUser)
								.set("user_pwd", deleteUser.getString("user_pwd"))
								.set("user_email", deleteUser.getString("user_email"))
								.set("user_profile", deleteUser.getString("user_profile"))
								.set("account_state", "DISABLED").set("user_role", deleteUser.getString("user_role"))
								.set("user_phone_number", deleteUser.getString("user_phone_number"))
								.set("user_housephone_number", deleteUser.getString("user_housephone_number"))
								.set("user_address", deleteUser.getString("user_address"))
								.set("user_address_comp", deleteUser.getString("user_address_comp"))
								.set("user_district", deleteUser.getString("user_district"))
								.set("user_cod_postal", deleteUser.getString("user_cod_postal")).build();

						txn.update(deleteUser);

						LOG.info("User removed " + data.deleteUser);
						txn.commit();
						return Response.ok("Removeu").build();

					} else if (deleteUser.getString("user_role").equals("GA")
							&& user.getString("user_role").equals("SU")) {

						deleteUser = Entity.newBuilder(deleteUserKey).set("user_name", data.deleteUser)
								.set("user_pwd", deleteUser.getString("user_pwd"))
								.set("user_email", deleteUser.getString("user_email"))
								.set("user_profile", deleteUser.getString("user_profile"))
								.set("account_state", "DISABLED").set("user_role", deleteUser.getString("user_role"))
								.set("user_phone_number", deleteUser.getString("user_phone_number"))
								.set("user_housephone_number", deleteUser.getString("user_housephone_number"))
								.set("user_address", deleteUser.getString("user_address"))
								.set("user_address_comp", deleteUser.getString("user_address_comp"))
								.set("user_district", deleteUser.getString("user_district"))
								.set("user_cod_postal", deleteUser.getString("user_cod_postal")).build();

						txn.update(deleteUser);

						LOG.info("User removed " + data.deleteUser);
						txn.commit();
						return Response.ok("Removeu").build();

					} else {
						txn.rollback();
						return Response.status(Status.BAD_REQUEST).entity("You don't have permissions for.").build();
					}
				} else {
					txn.rollback();
					return Response.status(Status.BAD_REQUEST).entity("Token is invalid.").build();
				}
			}
		} finally

		{
			if (txn.isActive())
				txn.rollback();
		}
	}

	@PUT
	@Path("/edit")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response editAtrributes(RegisterData data) {
		LOG.fine("Attempt to edit user: " + data.username);

		Transaction txn = datastore.newTransaction();

		try {
			Key userKey = userKeyFactory.newKey(data.username);
			Key tokenKey = tokenKeyFactory.newKey(data.username);
			Entity user = txn.get(userKey);
			Entity tok = txn.get(tokenKey);

			if (user == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong.").build();
			} else {

				if (tok.getString("tokenId").equals(data.tokenId)
						&& (tok.getLong("expirationData") > System.currentTimeMillis())) {

					if (user.getString("account_state").equals("ENABLED")) {

						if (user.getString("user_role").equals("USER")
								&& tok.getString("tokenId").equals(data.tokenId)) {

							user = Entity.newBuilder(userKey).set("user_name", user.getString("user_name"))
									.set("user_pwd", user.getString("user_pwd"))
									.set("user_email", user.getString("user_email"))
									.set("user_profile", user.getString("user_profile"))
									.set("account_state", user.getString("account_state"))
									.set("user_role", user.getString("user_role"))
									.set("user_phone_number", data.telemovel)
									.set("user_housephone_number", data.telFixo).set("user_address", data.morada)
									.set("user_address_comp", data.moradaComp).set("user_district", data.localidade)
									.set("user_cod_postal", data.codPostal).build();

							txn.update(user);

							LOG.info("User edited " + data.username);
							txn.commit();
							return Response.ok("Editou").build();
						} else {
							txn.rollback();
							return Response.status(Status.BAD_REQUEST).entity("You don't have permissions for.")
									.build();
						}
					} else {
						txn.rollback();
						return Response.status(Status.BAD_REQUEST).entity("This account has been disabled.").build();
					}
				} else {
					txn.rollback();
					return Response.status(Status.BAD_REQUEST).entity("Token is invalid.").build();
				}
			}
		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@PUT
	@Path("/role")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changeRole(RegisterData data) {
		LOG.fine("Attempt to edit role of user: " + data.username);

		Transaction txn = datastore.newTransaction();

		try {
			Key userKey = userKeyFactory.newKey(data.username);
			Key tokenKey = tokenKeyFactory.newKey(data.username);
			Key userRoleKey = userKeyFactory.newKey(data.changeUser);
			Key tokenRoleKey = tokenKeyFactory.newKey(data.changeUser);
			Entity user = txn.get(userKey);
			Entity userRole = txn.get(userRoleKey);
			Entity tok = txn.get(tokenKey);
			Entity tokRole = txn.get(tokenRoleKey);

			if (user == null || userRole == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong.").build();
			} else {

				if (tok.getString("tokenId").equals(data.tokenId)
						&& (tok.getLong("expirationData") > System.currentTimeMillis())) {

					if (user.getString("account_state").equals("ENABLED")
							&& userRole.getString("account_state").equals("ENABLED")) {

						if ((user.getString("user_role").equals("SU") || user.getString("user_role").equals("GA"))
								&& userRole.getString("user_role").equals("USER")) {

							if (data.role.equals("GBO") || data.role.equals("GA")) {

								userRole = Entity.newBuilder(userRoleKey)
										.set("user_name", userRole.getString("user_name"))
										.set("user_pwd", userRole.getString("user_pwd"))
										.set("user_email", userRole.getString("user_email"))
										.set("user_profile", userRole.getString("user_profile"))
										.set("account_state", userRole.getString("account_state"))
										.set("user_role", data.role)
										.set("user_phone_number", userRole.getString("user_phone_number"))
										.set("user_housephone_number", userRole.getString("user_housephone_number"))
										.set("user_address", userRole.getString("user_address"))
										.set("user_address_comp", userRole.getString("user_address_comp"))
										.set("user_district", userRole.getString("user_district"))
										.set("user_cod_postal", userRole.getString("user_cod_postal")).build();

								tokRole = Entity.newBuilder(tokenRoleKey).set("user_name", tok.getString("user_name"))
										.set("tokenId", tok.getString("tokenId"))
										.set("creation_data", tok.getLong("creation_data")).set("role", data.role)
										.set("expirationData", tok.getLong("expirationData")).build();

								txn.update(userRole);
								txn.update(tokRole);

								LOG.info("User edited " + data.username);
								txn.commit();
								return Response.ok("Editou").build();

							} else {
								txn.rollback();
								return Response.status(Status.BAD_REQUEST).entity("Invalid role.").build();
							}

						} else if (user.getString("user_role").equals("SU")
								&& (userRole.getString("user_role").equals("GBO")
										|| userRole.getString("user_role").equals("GA"))) {

							if (data.role.equals("GBO") || data.role.equals("GA")) {

								userRole = Entity.newBuilder(userRoleKey)
										.set("user_name", userRole.getString("user_name"))
										.set("user_pwd", userRole.getString("user_pwd"))
										.set("user_email", userRole.getString("user_email"))
										.set("user_profile", userRole.getString("user_profile"))
										.set("account_state", userRole.getString("account_state"))
										.set("user_role", data.role)
										.set("user_phone_number", userRole.getString("user_phone_number"))
										.set("user_housephone_number", userRole.getString("user_housephone_number"))
										.set("user_address", userRole.getString("user_address"))
										.set("user_address_comp", userRole.getString("user_address_comp"))
										.set("user_district", userRole.getString("user_district"))
										.set("user_cod_postal", userRole.getString("user_cod_postal")).build();

								tokRole = Entity.newBuilder(tokenRoleKey)
										.set("user_name", tokRole.getString("user_name"))
										.set("tokenId", tokRole.getString("tokenId"))
										.set("creation_data", tokRole.getLong("creation_data")).set("role", data.role)
										.set("expirationData", tokRole.getLong("expirationData")).build();

								txn.update(userRole);
								txn.update(tokRole);

								LOG.info("User edited " + data.username);
								txn.commit();
								return Response.ok("Editou").build();

							} else {
								txn.rollback();
								return Response.status(Status.BAD_REQUEST).entity("Invalid role.").build();
							}

						} else {
							txn.rollback();
							return Response.status(Status.BAD_REQUEST).entity("You don't have permission for.").build();
						}
					} else {
						txn.rollback();
						return Response.status(Status.BAD_REQUEST).entity("This account has been disabled.").build();
					}
				} else {
					txn.rollback();
					return Response.status(Status.BAD_REQUEST).entity("Token is invalid.").build();

				}
			}
		} finally {
			if (txn.isActive())
				txn.rollback();
		}

	}

	@PUT
	@Path("/state")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changeState(RegisterData data) {

		LOG.fine("Attempt to edit state of user: " + data.username);

		Transaction txn = datastore.newTransaction();

		try {
			Key userKey = userKeyFactory.newKey(data.username);
			Key tokenKey = tokenKeyFactory.newKey(data.username);
			Key userStateKey = userKeyFactory.newKey(data.changeUser);
			Entity user = txn.get(userKey);
			Entity userState = txn.get(userStateKey);
			Entity tok = txn.get(tokenKey);

			if (user == null || userState == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong.").build();
			} else {
				if (tok.getString("tokenId").equals(data.tokenId)
						&& (tok.getLong("expirationData") > System.currentTimeMillis())) {

					if (data.state.equals("ENABLED") || data.state.equals("DISABLED")) {

						if (!user.getString("user_role").equals("USER")) {

							if ((user.getString("user_role").equals("GBO") || user.getString("user_role").equals("GA")
									|| user.getString("user_role").equals("SU"))
									&& userState.getString("user_role").equals("USER")) {

								userState = Entity.newBuilder(userStateKey)
										.set("user_name", userState.getString("user_name"))
										.set("user_pwd", userState.getString("user_pwd"))
										.set("user_email", userState.getString("user_email"))
										.set("user_profile", userState.getString("user_profile"))
										.set("account_state", data.state)
										.set("user_role", userState.getString("user_role"))
										.set("user_phone_number", userState.getString("user_phone_number"))
										.set("user_housephone_number", userState.getString("user_housephone_number"))
										.set("user_address", userState.getString("user_address"))
										.set("user_address_comp", userState.getString("user_address_comp"))
										.set("user_district", userState.getString("user_district"))
										.set("user_cod_postal", userState.getString("user_cod_postal")).build();

								txn.update(userState);

								LOG.info("User edited " + data.username);
								txn.commit();
								return Response.ok("Editou").build();

							} else if ((user.getString("user_role").equals("GA")
									|| user.getString("user_role").equals("SU"))
									&& userState.getString("user_role").equals("GBO")) {

								userState = Entity.newBuilder(userStateKey)
										.set("user_name", userState.getString("user_name"))
										.set("user_pwd", userState.getString("user_pwd"))
										.set("user_email", userState.getString("user_email"))
										.set("user_profile", userState.getString("user_profile"))
										.set("account_state", data.state)
										.set("user_role", userState.getString("user_role"))
										.set("user_phone_number", userState.getString("user_phone_number"))
										.set("user_housephone_number", userState.getString("user_housephone_number"))
										.set("user_address", userState.getString("user_address"))
										.set("user_address_comp", userState.getString("user_address_comp"))
										.set("user_district", userState.getString("user_district"))
										.set("user_cod_postal", userState.getString("user_cod_postal")).build();

								txn.update(userState);

								LOG.info("User edited " + data.username);
								txn.commit();
								return Response.ok("Editou").build();

							} else if (user.getString("user_role").equals("SU")
									&& userState.getString("user_role").equals("GA")) {

								userState = Entity.newBuilder(userStateKey)
										.set("user_name", userState.getString("user_name"))
										.set("user_pwd", userState.getString("user_pwd"))
										.set("user_email", userState.getString("user_email"))
										.set("user_profile", userState.getString("user_profile"))
										.set("account_state", data.state)
										.set("user_role", userState.getString("user_role"))
										.set("user_phone_number", userState.getString("user_phone_number"))
										.set("user_housephone_number", userState.getString("user_housephone_number"))
										.set("user_address", userState.getString("user_address"))
										.set("user_address_comp", userState.getString("user_address_comp"))
										.set("user_district", userState.getString("user_district"))
										.set("user_cod_postal", userState.getString("user_cod_postal")).build();

								txn.update(userState);

								LOG.info("User edited " + data.username);
								txn.commit();
								return Response.ok("Editou").build();

							} else {
								txn.rollback();
								return Response.status(Status.BAD_REQUEST).entity("You don't have permission for.")
										.build();
							}
						} else {
							txn.rollback();
							return Response.status(Status.BAD_REQUEST).entity("You don't have permission for.").build();
						}
					} else {
						txn.rollback();
						return Response.status(Status.BAD_REQUEST).entity("State is invalid.").build();
					}
				} else {
					txn.rollback();
					return Response.status(Status.BAD_REQUEST).entity("Token is invalid.").build();
				}
			}
		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@PUT
	@Path("/pwd")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changePassword(RegisterData data) {

		LOG.fine("Attempt to edit pwd of user: " + data.username);

		Transaction txn = datastore.newTransaction();

		try {
			Key userKey = userKeyFactory.newKey(data.username);
			Key tokenKey = tokenKeyFactory.newKey(data.username);
			Entity user = txn.get(userKey);
			Entity tok = txn.get(tokenKey);

			if (user == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong.").build();
			} else {

				if (tok.getString("tokenId").equals(data.tokenId)
						&& (tok.getLong("expirationData") > System.currentTimeMillis())) {

					String hashedPwd = user.getString("user_pwd");

					if (hashedPwd.equals(DigestUtils.sha512Hex(data.pwd))) {

						if (data.newPwd.equals(data.newPwdConfirm) && data.newPwd.length() > 8) {

							user = Entity.newBuilder(userKey).set("user_name", user.getString("user_name"))
									.set("user_pwd", DigestUtils.sha512Hex(data.newPwd))
									.set("user_email", user.getString("user_email"))
									.set("user_profile", user.getString("user_profile"))
									.set("account_state", user.getString("account_state"))
									.set("user_role", user.getString("user_role"))
									.set("user_phone_number", user.getString("user_phone_number"))
									.set("user_housephone_number", user.getString("user_housephone_number"))
									.set("user_address", user.getString("user_address"))
									.set("user_address_comp", user.getString("user_address_comp"))
									.set("user_district", user.getString("user_district"))
									.set("user_cod_postal", user.getString("user_cod_postal")).build();

							txn.update(user);

							LOG.info("User edited " + data.username);
							txn.commit();
							return Response.ok("Editou").build();

						} else {
							txn.rollback();
							return Response.status(Status.BAD_REQUEST).entity("Data is invalid.").build();
						}
					} else {
						txn.rollback();
						return Response.status(Status.BAD_REQUEST).entity("Data is invalid.").build();
					}
				} else {
					txn.rollback();
					return Response.status(Status.BAD_REQUEST).entity("Token is invalid.").build();
				}
			}
		} finally {
			if (txn.isActive())
				txn.rollback();
		}
	}

	@POST
	@Path("/list")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response listUsersByRole(RegisterData data) {

		Transaction txn = datastore.newTransaction();

		try {
			Key userKey = userKeyFactory.newKey(data.username);
			Key tokenKey = tokenKeyFactory.newKey(data.username);
			Entity user = txn.get(userKey);
			Entity tok = txn.get(tokenKey);

			if (user == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong.").build();
			} else {

				if (tok.getString("tokenId").equals(data.tokenId)
						&& (tok.getLong("expirationData") > System.currentTimeMillis())) {

					if (user.getString("user_role").equals("GBO")) {

						if (data.role.equals("SU") || data.role.equals("GBO") || data.role.equals("GA")
								|| data.role.equals("USER")) {

							Query<Entity> query = Query.newEntityQueryBuilder().setKind("User")
									.setFilter(PropertyFilter.eq("user_role", data.role)).build();

							QueryResults<Entity> roles = datastore.run(query);

							List<String> userRoles = new ArrayList();

							while (roles.hasNext()) {
								Entity role = roles.next();
								userRoles.add(role.getString("user_name"));
							}

							txn.commit();
							return Response.ok(g.toJson(userRoles)).build();

						} else {
							txn.rollback();
							return Response.status(Status.BAD_REQUEST).entity("Role is invalid.").build();
						}
					} else {
						txn.rollback();
						return Response.status(Status.BAD_REQUEST).entity("You don't have permission for.").build();
					}
				} else {
					txn.rollback();
					return Response.status(Status.BAD_REQUEST).entity("Token is invalid.").build();
				}
			}
		} finally {
			if (txn.isActive())
				txn.rollback();
		}

	}

}
