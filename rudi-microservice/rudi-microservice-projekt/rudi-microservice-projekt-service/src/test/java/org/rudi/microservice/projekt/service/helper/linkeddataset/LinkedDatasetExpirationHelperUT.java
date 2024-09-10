package org.rudi.microservice.projekt.service.helper.linkeddataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.io.IOException;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.rudi.common.core.json.JsonResourceReader;
import org.rudi.microservice.projekt.service.ProjectSpringBootTest;
import org.rudi.microservice.projekt.storage.dao.linkeddataset.LinkedDatasetDao;
import org.rudi.microservice.projekt.storage.entity.linkeddataset.LinkedDatasetEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import lombok.RequiredArgsConstructor;
import lombok.val;

@ProjectSpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LinkedDatasetExpirationHelperUT {
	private static final String JSON_EXPIRED = "linkeddatasets/linked_dataset_expired.json";
	private static final String JSON_NOT_EXPIRED = "linkeddatasets/linked_dataset_not_expired.json";
	private static final String JSON_WITHOUT_END_DATE = "linkeddatasets/linked_dataset_without_end_date.json";

	private final LinkedDatasetDao linkedDatasetDao;
	private final JsonResourceReader jsonResourceReader;
	private final LinkedDatasetExpirationHelper linkedDatasetExpirationHelper;
	@MockBean
	private final LinkedDatasetSubscriptionHelper linkedDatasetSubscriptionHelper;

	private LinkedDatasetEntity createLinkedDatasetFromJson(String jsonPath) throws IOException {
		final LinkedDatasetEntity linkedDataset = jsonResourceReader.read(jsonPath, LinkedDatasetEntity.class);
		return linkedDatasetDao.save(linkedDataset);
	}

	@Test
	void getValidatedLinkedDatasetExpired_none_expired() throws IOException {
		createLinkedDatasetFromJson(JSON_NOT_EXPIRED);

		val result = linkedDatasetExpirationHelper.getValidatedLinkedDatasetExpired();

		assertThat(result.size()).isEqualTo(0);
	}

	@Test
	void getValidatedLinkedDatasetExpired_some_expired() throws IOException {
		createLinkedDatasetFromJson(JSON_NOT_EXPIRED);
		createLinkedDatasetFromJson(JSON_EXPIRED);

		val result = linkedDatasetExpirationHelper.getValidatedLinkedDatasetExpired();

		assertThat(CollectionUtils.isNotEmpty(result)).isTrue();
	}

	@Test
	void getValidatedLinkedDatasetExpired_find_which_expired() throws IOException {
		createLinkedDatasetFromJson(JSON_NOT_EXPIRED);
		val expiredLinkedDataset = createLinkedDatasetFromJson(JSON_EXPIRED);

		val result = linkedDatasetExpirationHelper.getValidatedLinkedDatasetExpired();

		assertThat(CollectionUtils.isNotEmpty(result)).isTrue();

		val expiredFromResult = result.get(0);

		assertThat(expiredFromResult.getUuid()).isEqualTo(expiredLinkedDataset.getUuid());
	}

	@Test
	void getValidatedLinkedDatasetWithoutEndDate_none_without_end_date() throws IOException {
		createLinkedDatasetFromJson(JSON_NOT_EXPIRED);
		createLinkedDatasetFromJson(JSON_EXPIRED);

		val result = linkedDatasetExpirationHelper.getRestrictedValidatedLinkedDatasetWithoutEndDate();

		assertThat(CollectionUtils.isEmpty(result)).isTrue();
	}

	@Test
	void getValidatedLinkedDatasetWithoutEndDate_some_without_end_date() throws IOException {
		createLinkedDatasetFromJson(JSON_NOT_EXPIRED);
		createLinkedDatasetFromJson(JSON_EXPIRED);
		val withoutEndDate = createLinkedDatasetFromJson(JSON_WITHOUT_END_DATE);

		val result = linkedDatasetExpirationHelper.getRestrictedValidatedLinkedDatasetWithoutEndDate();

		assertThat(CollectionUtils.isNotEmpty(result)).isTrue();
		assertThat(result.get(0).getUuid()).isEqualByComparingTo(withoutEndDate.getUuid());
	}

	@Test
	void cleanLinkedDatasetExpired_without_error() throws IOException {
		doNothing().when(linkedDatasetSubscriptionHelper).handleUnlinkLinkedDataset(any());
		createLinkedDatasetFromJson(JSON_NOT_EXPIRED);
		createLinkedDatasetFromJson(JSON_EXPIRED);
		val listToClean = linkedDatasetExpirationHelper.getValidatedLinkedDatasetExpired();
		Assertions.assertThatCode(() -> linkedDatasetExpirationHelper.cleanLinkedDatasetExpired(listToClean))
				.doesNotThrowAnyException();
	}

	@AfterEach
	void tearDown() {
		linkedDatasetDao.deleteAll();
	}
}
