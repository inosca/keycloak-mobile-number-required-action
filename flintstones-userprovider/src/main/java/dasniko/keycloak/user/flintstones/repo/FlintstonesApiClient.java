package dasniko.keycloak.user.flintstones.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import dasniko.keycloak.user.flintstones.FlintstonesUserStorageProviderFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.util.List;

@Slf4j
public class FlintstonesApiClient {

	private final CloseableHttpClient httpClient;
	private final String baseUrl;

	public FlintstonesApiClient(KeycloakSession session, ComponentModel model) {
		this.httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
		this.baseUrl = model.get(FlintstonesUserStorageProviderFactory.USER_API_BASE_URL);
	}

	@SneakyThrows
	public List<FlintstoneUser> searchUsers(String search, int first, int max) {
		String url = String.format("%s/users", baseUrl);
		SimpleHttp simpleHttp = prepareGetRequest(url);
		if (first >= 0) {
			simpleHttp.param("first", String.valueOf(first));
		}
		if (max >= 0) {
			simpleHttp.param("max", String.valueOf(max));
		}
		if (search != null) {
			simpleHttp.param("search", search);
		}
		return simpleHttp.asJson(new TypeReference<>() {});
	}

	@SneakyThrows
	public Integer usersCount() {
		String url = String.format("%s/users/count", baseUrl);
		String count = prepareGetRequest(url).asString();
		return Integer.valueOf(count);
	}

	@SneakyThrows
	public boolean createUser(FlintstoneUser user) {
		String url = String.format("%s/users", baseUrl);
		return SimpleHttp.doPost(url, httpClient).json(user).asStatus() == 201;
	}

	@SneakyThrows
	public FlintstoneUser getUserById(String userId) {
		String url = String.format("%s/users/%s", baseUrl, userId);
		try (SimpleHttp.Response response = prepareGetRequest(url).asResponse()) {
			if (response.getStatus() >= 400) {
				return null;
			}
			return response.asJson(FlintstoneUser.class);
		}
	}

	public FlintstoneUser getUserByUsername(String username) {
		return getUserByUsernameOrEmail("username", username);
	}

	public FlintstoneUser getUserByEmail(String email) {
		return getUserByUsernameOrEmail("email", email);
	}

	@SneakyThrows
	private FlintstoneUser getUserByUsernameOrEmail(String field, String value) {
		String url = String.format("%s/users", baseUrl);
		SimpleHttp simpleHttp = prepareGetRequest(url);
		simpleHttp.param(field, value);
		List<FlintstoneUser> result = simpleHttp.asJson(new TypeReference<>() {});
		return result.isEmpty() ? null : result.get(0);
	}

	@SneakyThrows
	public boolean updateUser(FlintstoneUser user) {
		String url = String.format("%s/users/%s", baseUrl, user.getId());
		return SimpleHttp.doPut(url, httpClient).json(user).asStatus() == 204;
	}

	@SneakyThrows
	public boolean deleteUser(String userId) {
		String url = String.format("%s/users/%s", baseUrl, userId);
		return SimpleHttp.doDelete(url, httpClient).asStatus() == 204;
	}

	@SneakyThrows
	public boolean verifyCredentials(String userId, Credential credential) {
		String url = String.format("%s/users/%s/credentials/verify", baseUrl, userId);
		return SimpleHttp.doPost(url, httpClient).json(credential).asStatus() == 204;
	}

	@SneakyThrows
	public boolean updateCredentials(String userId, Credential credential) {
		String url = String.format("%s/users/%s/credentials", baseUrl, userId);
		return SimpleHttp.doPut(url, httpClient).json(credential).asStatus() == 204;
	}

	private SimpleHttp prepareGetRequest(String url) {
		return SimpleHttp.doGet(url, httpClient).acceptJson();
	}
}
