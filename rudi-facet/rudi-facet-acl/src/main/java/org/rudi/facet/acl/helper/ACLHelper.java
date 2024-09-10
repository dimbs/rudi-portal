package org.rudi.facet.acl.helper;

import static org.rudi.facet.acl.helper.ACLConstants.PROJECT_KEY_STORE_UUID_PARAMETER;
import static org.rudi.facet.acl.helper.ACLConstants.PROJECT_KEY_UUID_PARAMETER;
import static org.rudi.facet.acl.helper.ACLConstants.USER_LOGIN_AND_DENOMINATION_PARAMETER;
import static org.rudi.facet.acl.helper.ACLConstants.USER_TYPE_PARAMETER;
import static org.rudi.facet.acl.helper.ACLConstants.USER_UUIDS_PARAMETER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.rudi.common.core.security.AuthenticatedUser;
import org.rudi.common.service.exception.AppServiceUnauthorizedException;
import org.rudi.common.service.helper.UtilContextHelper;
import org.rudi.facet.acl.bean.AddressType;
import org.rudi.facet.acl.bean.EmailAddress;
import org.rudi.facet.acl.bean.PasswordUpdate;
import org.rudi.facet.acl.bean.ProjectKey;
import org.rudi.facet.acl.bean.ProjectKeystore;
import org.rudi.facet.acl.bean.ProjectKeystorePageResult;
import org.rudi.facet.acl.bean.Role;
import org.rudi.facet.acl.bean.User;
import org.rudi.facet.acl.bean.UserPageResult;
import org.rudi.facet.acl.bean.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import reactor.core.publisher.Mono;

/**
 * L'utilisation de ce helper requiert l'ajout de 2 propriétés dans le fichier de configuration associé
 *
 * @author FNI18300
 */
@Component
@RequiredArgsConstructor
public class ACLHelper {

	private static final String ORDER_PARAMETER = "order";

	private static final String OFFSET_PARAMETER = "offset";

	private static final Logger LOGGER = LoggerFactory.getLogger(ACLHelper.class);

	private static final String LIMIT_PARAMETER = "limit";
	private static final String ACTIVE_PARAMETER = "active";
	private static final String CODE_PARAMETER = "code";

	public static final String LOGIN_REQUIRED = "login required";

	@Getter
	@Value("${rudi.facet.acl.endpoint.users.search.url:/acl/v1/roles}")
	private String rolesEndpointSearchURL;

	@Getter
	@Value("${rudi.facet.acl.endpoint.roles.search.url:/acl/v1/users}")
	private String usersEndpointSearchURL;

	@Getter
	@Value("${rudi.facet.acl.endpoint.users.crud.url:/acl/v1/users}")
	private String userEndpointCRUDURL;

	@Getter
	@Value("${rudi.facet.acl.endpoint.users.count.url:/acl/v1/users/count}")
	private String userCountEndpointURL;

	@Getter
	@Value("${rudi.facet.acl.endpoint.users.client-registration-by-password.url:/acl/v1/users/{login}/password}")
	private String updateUserPasswordEndpointUrl;

	@Getter
	@Value("${rudi.facet.acl.endpoint.project-key-stores.search.url:/acl/v1/project-keystores}")
	private String searchOrCreateProjectKeystoresUrl;

	@Getter
	@Value("${rudi.facet.acl.endpoint.project-key-stores.get.url:/acl/v1/project-keystores/{project-key-store-uuid}}")
	private String getOrDeleteProjectKeystoresUrl;
	@Getter
	@Value("${rudi.facet.acl.endpoint.project-key-stores.create.url:/acl/v1/project-keystores/{project-key-store-uuid}/project-keys}")
	private String createProjectKeyUrl;

	@Getter
	@Value("${rudi.facet.acl.endpoint.project-key-stores.delete.url:/acl/v1/project-keystores/{project-key-store-uuid}/project-keys/{project-key-uuid}}")
	private String deleteProjectKeyUrl;

	@Getter
	@Value("${rudi.facet.acl.service.url:lb://RUDI-ACL/}")
	private String aclServiceURL;

	@Autowired
	@Qualifier("rudi_oauth2")
	private WebClient loadBalancedWebClient;

	private final UtilContextHelper utilContextHelper;

	/**
	 * Accède au service µACL pour trouver un utilisateur par son uuid
	 *
	 * @return l'utilisateur, <code>null</code> si l'utilisateur n'existe pas
	 */
	@Nullable
	public User getUserByUUID(UUID userUuid) {
		return getMonoUserByUUID(userUuid).block();
	}

	@Nullable
	public Mono<User> getMonoUserByUUID(UUID userUuid) {
		if (userUuid == null) {
			throw new IllegalArgumentException("user uuid required");
		}
		return loadBalancedWebClient.get().uri(buildUsersGetDeleteURL(userUuid)).retrieve().bodyToMono(User.class);
	}

	/**
	 * Accède au service µProviders pour trouver un provider par son uuid
	 */
	public void deleteUserByUUID(UUID userUuid) {
		if (userUuid == null) {
			throw new IllegalArgumentException("user uuid required");
		}
		ClientResponse response = loadBalancedWebClient.delete().uri(buildUsersGetDeleteURL(userUuid))
				.exchangeToMono(Mono::just).block();
		if (response != null && response.statusCode() != HttpStatus.OK) {
			LOGGER.warn("Failed to delete user :{}", userUuid);
		}
	}

	/**
	 * Accède au service µProviders pour trouver un provider par l'uuid d'un de ses noeuds
	 *
	 * @return le provider correspondant
	 */
	@Nullable
	public User getUserByLogin(String login) {
		if (login == null) {
			throw new IllegalArgumentException(LOGIN_REQUIRED);
		}

		final var criteria = UserSearchCriteria.builder().login(login).build();
		return getUser(criteria);
	}

	/**
	 * Accède au service ACL pour trouver un utilisateur par son login et son mot-de-passe
	 *
	 * @return l'utilisateur correspondant, null sinon
	 */
	@Nullable
	public User getUserByLoginAndPassword(String login, String password) {
		if (login == null) {
			throw new IllegalArgumentException(LOGIN_REQUIRED);
		}
		if (password == null) {
			throw new IllegalArgumentException("password required");
		}

		final var criteria = UserSearchCriteria.builder().login(login).password(password).build();
		return getUser(criteria);
	}

	/**
	 * @return l'utilisateur correspondant aux critères, null sinon
	 */
	@Nullable
	private User getUser(UserSearchCriteria criteria) {
		UserPageResult pageResult = getMonoUsers(criteria).block();
		if (pageResult != null && CollectionUtils.isNotEmpty(pageResult.getElements())) {
			return pageResult.getElements().get(0);
		} else {
			return null;
		}
	}

	/**
	 * @return l'utilisateur correspondant aux critères, null sinon
	 */
	@Nullable
	public Mono<UserPageResult> getMonoUsers(UserSearchCriteria criteria) {

		return loadBalancedWebClient.get().uri(buildUsersSearchURL(), uriBuilder -> uriBuilder
				.queryParam(LIMIT_PARAMETER, 1)
				.queryParamIfPresent(ACLConstants.USER_LOGIN_PARAMETER, Optional.ofNullable(criteria.getLogin()))
				.queryParamIfPresent(ACLConstants.USER_PASSWORD_PARAMETER, Optional.ofNullable(criteria.getPassword()))
				.queryParam(ACLConstants.ROLE_UUIDS_PARAMETER, formatListParameter(criteria.getRoleUuids())).build())
				.retrieve().bodyToMono(UserPageResult.class);
	}

	@Nullable
	private String formatListParameter(List<UUID> uuidList) {
		String result = null;
		if (CollectionUtils.isNotEmpty(uuidList)) {
			result = uuidList.stream().map(UUID::toString).collect(Collectors.joining(","));
		}

		return result;
	}

	public User createUser(User user) {
		User result = null;
		if (user == null) {
			throw new IllegalArgumentException("user required");
		}

		result = loadBalancedWebClient.post().uri(buildUsersPostPutURL()).bodyValue(user).retrieve()
				.bodyToMono(User.class).block();
		return result;
	}

	public User updateUser(User user) {
		User result = null;
		if (user == null) {
			throw new IllegalArgumentException("user required");
		}

		result = loadBalancedWebClient.put().uri(buildUsersPostPutURL()).bodyValue(user).retrieve()
				.bodyToMono(User.class).block();
		return result;
	}

	/**
	 * Accède au service µProviders pour trouver un provider par l'uuid d'un de ses noeuds
	 *
	 * @return le provider correspondant
	 */
	public List<Role> searchRoles() {
		Role[] roles = loadBalancedWebClient.get().uri(buildRolesSearchURL(null)).retrieve().bodyToMono(Role[].class)
				.block();
		return Arrays.stream(roles).collect(Collectors.toList());
	}

	/**
	 * Retourne la liste des utilisateurs associé à un rôle dont le code est passé en paramètre
	 *
	 * @param roleCode code du Role sur lequel filtrer
	 * @return List de User ayant le rôle. Si Aucun, list Vide
	 */
	@NotNull
	public List<User> searchUsers(String roleCode) {
		List<User> result = new ArrayList<>();
		Role[] roles = loadBalancedWebClient.get().uri(buildRolesSearchURL(roleCode)).retrieve()
				.bodyToMono(Role[].class).block();
		if (ArrayUtils.isNotEmpty(roles)) {
			final var criteria = UserSearchCriteria.builder()
					.roleUuids(Arrays.stream(roles).map(Role::getUuid).collect(Collectors.toList())).build();

			// Retrait de la limit à 1 : envoie de mail au role MODERATOR
			// et pas seulement à 1 membre ayant le role MODERATOR
			UserPageResult pageResult = loadBalancedWebClient.get()
					.uri(buildUsersSearchURL(),
							uriBuilder -> uriBuilder.queryParam(ACLConstants.ROLE_UUIDS_PARAMETER,
									formatListParameter(criteria.getRoleUuids())).build())
					.retrieve().bodyToMono(UserPageResult.class).block();

			if (pageResult != null && pageResult.getElements() != null) {
				result.addAll(pageResult.getElements());
			}
		}
		return result;
	}

	@NotNull
	public List<User> searchUsersWithCriteria(List<UUID> userUuids, @Nullable String searchText, @Nullable String type,
			@Nullable Integer limit) {
		List<User> result = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(userUuids)) {
			if (searchText == null) {
				searchText = "";
			}

			UserType valueType = null;
			if (StringUtils.isNotBlank(type)) {
				try {
					valueType = UserType.fromValue(type);
				} catch (IllegalArgumentException e) {
					// ignore the wrong value no error on call
				}
			}

			final var criteria = UserSearchCriteria.builder().userUuids(userUuids).loginAndDenomination(searchText)
					.userType(valueType).build();
			UserPageResult pageResult = loadBalancedWebClient.get()
					.uri(buildUsersSearchURL(), uriBuilder -> uriBuilder
							.queryParam(USER_UUIDS_PARAMETER, formatListParameter(criteria.getUserUuids()))
							.queryParam(USER_LOGIN_AND_DENOMINATION_PARAMETER, criteria.getLoginAndDenomination())
							.queryParam(USER_TYPE_PARAMETER,
									criteria.getUserType() != null ? criteria.getUserType().toString() : null)
							.queryParam(LIMIT_PARAMETER, limit).build())
					.retrieve().bodyToMono(UserPageResult.class).block();
			if (pageResult != null && pageResult.getElements() != null) {
				result.addAll(pageResult.getElements());
			}
		}
		return result;
	}

	protected String buildUsersGetDeleteURL(UUID value) {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getAclServiceURL()).append(getUserEndpointCRUDURL()).append('/').append(value);
		return urlBuilder.toString();
	}

	protected String buildUsersPostPutURL() {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getAclServiceURL()).append(getUserEndpointCRUDURL());
		return urlBuilder.toString();
	}

	protected String buildUsersSearchURL() {
		return getAclServiceURL() + getUsersEndpointSearchURL();
	}

	protected String buildUsersCountURL() {
		return getAclServiceURL() + getUserCountEndpointURL();
	}

	protected String buildRolesSearchURL(String code) {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getAclServiceURL()).append(getRolesEndpointSearchURL());
		urlBuilder.append("?").append(ACTIVE_PARAMETER).append('=').append(true);
		if (StringUtils.isNotEmpty(code)) {
			urlBuilder.append("&").append(CODE_PARAMETER).append('=').append(code);
		}
		return urlBuilder.toString();
	}

	protected String buildUpdateUserPasswordURL() {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getAclServiceURL()).append(getUpdateUserPasswordEndpointUrl());
		return urlBuilder.toString();
	}

	public String lookupEMailAddress(User user) {
		String result = null;
		if (CollectionUtils.isNotEmpty(user.getAddresses())) {
			result = user.getAddresses().stream().filter(a -> a.getType() == AddressType.EMAIL)
					.map(a -> ((EmailAddress) a).getEmail()).findFirst().orElse(null);
		}
		if (StringUtils.isEmpty(result) && user.getLogin().contains("@")) {
			return user.getLogin();
		}
		return result;
	}

	public List<String> lookupEmailAddresses(List<User> users) {
		List<String> result = new ArrayList<>();
		for (User user : users) {
			String email = lookupEMailAddress(user);
			if (StringUtils.isNotEmpty(email)) {
				result.add(email);
			}
		}
		return result;
	}

	protected boolean andOrWhat(StringBuilder urlBuilder, boolean and) {
		if (and) {
			urlBuilder.append("&");
		} else {
			urlBuilder.append("?");
		}
		return true;
	}

	@Nonnull
	public UUID getAuthenticatedUserUuid() throws AppServiceUnauthorizedException {
		return getAuthenticatedUser().getUuid();
	}

	@Nonnull
	public User getAuthenticatedUser() throws AppServiceUnauthorizedException {
		val authenticatedUser = utilContextHelper.getAuthenticatedUser();
		if (authenticatedUser == null) {
			throw new AppServiceUnauthorizedException("No authenticated user");
		}
		return lookupUser(authenticatedUser);
	}

	private User lookupUser(AuthenticatedUser authenticatedUser) throws AppServiceUnauthorizedException {
		val user = getUserByLogin(authenticatedUser.getLogin());
		if (user == null) {
			throw new AppServiceUnauthorizedException(String
					.format("Authenticated user with login \"%s\" user is unknown", authenticatedUser.getLogin()));
		}
		return user;
	}

	/**
	 * Permet la modification du mot de passe d'un utilisateur.
	 *
	 * @param login       de l'utilisateur
	 * @param oldPassword contient l'ancien mot de passe
	 * @param newPassword contient le nouveau mot de passe
	 */
	public void updateUserPassword(String login, String oldPassword, String newPassword)
			throws WebClientResponseException {
		if (login == null) {
			throw new IllegalArgumentException(LOGIN_REQUIRED);
		}
		if (oldPassword == null) {
			throw new IllegalArgumentException("old password required");
		}
		if (newPassword == null) {
			throw new IllegalArgumentException("new password required");
		}
		PasswordUpdate passwordUpdate = new PasswordUpdate();
		passwordUpdate.setNewPassword(newPassword);
		passwordUpdate.setOldPassword(oldPassword);

		loadBalancedWebClient.put().uri(buildUpdateUserPasswordURL(), Map.of(ACLConstants.USER_LOGIN_PARAMETER, login))
				.bodyValue(passwordUpdate).retrieve().toBodilessEntity().block();
	}

	public Long getUserCount() {
		Long userCount = loadBalancedWebClient.get().uri(buildUsersCountURL()).retrieve().bodyToMono(Long.class)
				.block();
		if (userCount != null) {
			return userCount;
		}
		return 0L;
	}

	protected String buildSearchOrCreateProjectKeystoreUrl() {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getAclServiceURL()).append(getSearchOrCreateProjectKeystoresUrl());
		return urlBuilder.toString();
	}

	protected String buildGetOrDeleteProjectKeystoresUrl() {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getAclServiceURL()).append(getGetOrDeleteProjectKeystoresUrl());
		return urlBuilder.toString();
	}

	protected String buildCreateProjectKeyUrl() {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getAclServiceURL()).append(getCreateProjectKeyUrl());
		return urlBuilder.toString();
	}

	protected String buildDeleteProjectKeyUrl() {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(getAclServiceURL()).append(getDeleteProjectKeyUrl());
		return urlBuilder.toString();
	}

	public ProjectKeystore createProjectKeyStore(UUID projectUuid) {
		ProjectKeystore input = new ProjectKeystore();
		input.setProjectUuid(projectUuid);
		return loadBalancedWebClient.post().uri(buildSearchOrCreateProjectKeystoreUrl()).bodyValue(input).retrieve()
				.bodyToMono(ProjectKeystore.class).block();
	}

	public ProjectKeystore getProjectKeyStore(UUID projectKeystoreUuid) {
		return loadBalancedWebClient.get()
				.uri(buildGetOrDeleteProjectKeystoresUrl(),
						Map.of(PROJECT_KEY_STORE_UUID_PARAMETER, projectKeystoreUuid))
				.retrieve().bodyToMono(ProjectKeystore.class).block();
	}

	public void deleteProjectKeyStore(UUID projectKeystoreUuid) {
		loadBalancedWebClient.delete()
				.uri(buildGetOrDeleteProjectKeystoresUrl(),
						Map.of(PROJECT_KEY_STORE_UUID_PARAMETER, projectKeystoreUuid))
				.retrieve().toBodilessEntity().block();
	}

	public ProjectKey createProjectKey(UUID projectKeyStoreUuid, ProjectKey projectKey) {
		return loadBalancedWebClient.put()
				.uri(buildCreateProjectKeyUrl(), Map.of(PROJECT_KEY_STORE_UUID_PARAMETER, projectKeyStoreUuid))
				.bodyValue(projectKey).retrieve().bodyToMono(ProjectKey.class).block();
	}

	public void deleteProjectKey(UUID projectKeyStoreUuid, UUID projectKeyUuid) {
		loadBalancedWebClient.post().uri(buildDeleteProjectKeyUrl(), Map.of(PROJECT_KEY_STORE_UUID_PARAMETER,
				projectKeyStoreUuid, PROJECT_KEY_UUID_PARAMETER, projectKeyUuid)).retrieve().toBodilessEntity().block();
	}

	/**
	 * @param searchCriteria
	 * @param page
	 * @return
	 */
	public Page<ProjectKeystore> searchProjectKeystores(ProjectKeystoreSearchCriteria searchCriteria, Pageable page) {
		ProjectKeystorePageResult projectKeystorePageResult = searchMonoProjectKeystores(searchCriteria, page).block();
		return new PageImpl<>(projectKeystorePageResult != null ? projectKeystorePageResult.getElements() : List.of(),
				page, projectKeystorePageResult != null ? projectKeystorePageResult.getTotal() : 0);
	}

	/**
	 * @param searchCriteria
	 * @param page
	 * @return
	 */
	public Mono<ProjectKeystorePageResult> searchMonoProjectKeystores(ProjectKeystoreSearchCriteria searchCriteria,
			Pageable page) {
		return loadBalancedWebClient.get().uri(buildSearchOrCreateProjectKeystoreUrl(), uriBuilder -> uriBuilder
				.queryParamIfPresent("client-id", Optional.ofNullable(searchCriteria.getClientId()))
				.queryParamIfPresent("min-expiration-date", Optional.ofNullable(searchCriteria.getMinExpirationDate()))
				.queryParamIfPresent("max-expiration-date", Optional.ofNullable(searchCriteria.getMaxExpirationDate()))
				.queryParamIfPresent("project-uuids", Optional.ofNullable(searchCriteria.getProjectUuids()))
				.queryParamIfPresent(OFFSET_PARAMETER, Optional.ofNullable(page.getOffset()))
				.queryParamIfPresent(LIMIT_PARAMETER, Optional.ofNullable(page.getPageSize()))
				.queryParamIfPresent(ORDER_PARAMETER, Optional.ofNullable(convertSort(page.getSort()))).build())
				.retrieve().bodyToMono(ProjectKeystorePageResult.class);
	}

	protected String convertSort(Sort sort) {
		if (sort == null || sort.isUnsorted()) {
			return null;
		}
		StringBuilder sortBuilder = new StringBuilder();
		sort.forEach(order -> {
			if (sortBuilder.length() > 0) {
				sortBuilder.append(',');
			}
			if (order.isDescending()) {
				sortBuilder.append('-');
			}
			sortBuilder.append(order.getProperty());
		});
		return sortBuilder.toString();
	}

}
