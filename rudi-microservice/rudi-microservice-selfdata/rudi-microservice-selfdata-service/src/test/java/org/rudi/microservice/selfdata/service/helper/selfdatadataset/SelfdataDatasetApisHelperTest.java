package org.rudi.microservice.selfdata.service.helper.selfdatadataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rudi.common.core.security.AuthenticatedUser;
import org.rudi.common.service.exception.AppServiceBadRequestException;
import org.rudi.common.service.exception.AppServiceException;
import org.rudi.common.service.exception.AppServiceNotFoundException;
import org.rudi.facet.apigateway.exceptions.GetApiException;
import org.rudi.facet.apigateway.helper.ApiGatewayHelper;
import org.rudi.facet.dataset.bean.InterfaceContract;
import org.rudi.facet.kaccess.bean.Connector;
import org.rudi.facet.kaccess.bean.Media;
import org.rudi.facet.kaccess.bean.MediaType;
import org.rudi.facet.kaccess.bean.Metadata;
import org.rudi.facet.kaccess.bean.MetadataAccessCondition;
import org.rudi.facet.kaccess.bean.MetadataAccessConditionConfidentiality;
import org.rudi.facet.kaccess.bean.MetadataExtMetadata;
import org.rudi.facet.kaccess.bean.MetadataExtMetadataExtSelfdata;
import org.rudi.facet.kaccess.bean.SelfdataContent;
import org.rudi.microservice.apigateway.core.bean.Api;
import org.rudi.microservice.apigateway.core.bean.ApiMethod;
import org.rudi.microservice.selfdata.core.bean.BarChartData;
import org.rudi.microservice.selfdata.core.bean.GenericDataObject;
import org.rudi.microservice.selfdata.service.SelfdataSpringBootTest;
import org.rudi.microservice.selfdata.service.exception.InvalidSelfdataApisException;
import org.rudi.microservice.selfdata.service.exception.MissingApiForMediaException;
import org.rudi.microservice.selfdata.service.helper.apigateway.SelfdataApiGatewayHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;

import lombok.RequiredArgsConstructor;

@SelfdataSpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SelfdataDatasetApisHelperTest {

	private final SelfdataDatasetApisHelper selfdataDatasetApisHelper;

	@MockBean
	private ApiGatewayHelper apiGatewayHelper;

	@MockBean
	private SelfdataApiGatewayHelper selfdataApiGatewayHelper;

	@ParameterizedTest
	@MethodSource("getMalformedMetadatas")
	void test_getGdataMedia_404_on_malformed_metadata(Metadata input) {
		assertThrows(InvalidSelfdataApisException.class, () -> selfdataDatasetApisHelper.getGdataMedia(input));
	}

	@ParameterizedTest
	@MethodSource("getMalformedMetadatas")
	void test_geTpbcMedia_404_on_malformed_metadata(Metadata input) {
		assertThrows(InvalidSelfdataApisException.class, () -> selfdataDatasetApisHelper.getTpbcMedia(input));
	}

	private static Stream<Arguments> getMalformedMetadatas() {
		return Stream.of(Arguments.of(new Metadata()), Arguments.of(getMetadataWithoutApis()),
				Arguments.of(getMetadataWithWrongApis()), Arguments.of(getMetadataWithTooMuchGdata()),
				Arguments.of(getMetadataWithTooMuchTpbc()));
	}

	@Test
	void test_getGdataMedia_500_on_invalid_metadata() {
		assertThrows(AppServiceException.class, () -> selfdataDatasetApisHelper.getGdataMedia(null));
	}

	@Test
	void test_getTpbcMedia_500_on_invalid_metadata() {
		assertThrows(AppServiceException.class, () -> selfdataDatasetApisHelper.getTpbcMedia(null));
	}

	@ParameterizedTest
	@MethodSource("getInvalidApiParams")
	void test_getGdataData_ko_when_invalidParameters(SelfdataApiParameters parameters) {
		assertThrows(AppServiceException.class, () -> selfdataDatasetApisHelper.getGdataData(parameters));
	}

	@ParameterizedTest
	@MethodSource("getInvalidApiParams")
	void test_getTpbcData_ko_when_invalidParameters(SelfdataApiParameters parameters) {
		assertThrows(AppServiceException.class, () -> selfdataDatasetApisHelper.getTpbcData(parameters));
	}

	private static Stream<Arguments> getInvalidApiParams() {

		SelfdataApiParameters justMetadata = new SelfdataApiParameters();
		justMetadata.setMetadata(new Metadata());

		SelfdataApiParameters justUser = new SelfdataApiParameters();
		justMetadata.setUser(new AuthenticatedUser());

		SelfdataApiParameters justApplication = new SelfdataApiParameters();

		SelfdataApiParameters metadataAndUser = new SelfdataApiParameters();
		metadataAndUser.setMetadata(new Metadata());
		justMetadata.setUser(new AuthenticatedUser());

		SelfdataApiParameters userAndApplication = new SelfdataApiParameters();
		userAndApplication.setUser(new AuthenticatedUser());

		SelfdataApiParameters metadataAndApplication = new SelfdataApiParameters();
		metadataAndApplication.setMetadata(new Metadata());

		SelfdataApiParameters allButEmpty = new SelfdataApiParameters();
		allButEmpty.setMetadata(new Metadata());
		allButEmpty.setUser(new AuthenticatedUser());

		return Stream.of(null, Arguments.of(new SelfdataApiParameters()), Arguments.of(justMetadata),
				Arguments.of(justUser), Arguments.of(justApplication), Arguments.of(justMetadata),
				Arguments.of(userAndApplication), Arguments.of(metadataAndApplication), Arguments.of(allButEmpty));
	}

	@ParameterizedTest
	@MethodSource("getMissingApiGatewayResponses")
	void test_getGdataData_ko_when_MediaMissingApigatewayApi(Api inputApi) throws GetApiException {

		SelfdataApiParameters parameters = createValidParameters();

		when(apiGatewayHelper.getApiById(any(), any())).thenReturn(inputApi);
		assertThrows(MissingApiForMediaException.class, () -> selfdataDatasetApisHelper.getGdataData(parameters));
	}

	@ParameterizedTest
	@MethodSource("getMissingApiGatewayResponses")
	void test_getTpbcData_ko_when_MediaMissingApigatewayApi(Api inputApi) throws GetApiException {

		SelfdataApiParameters parameters = createValidParameters();

		when(apiGatewayHelper.getApiById(any(), any())).thenReturn(inputApi);
		assertThrows(MissingApiForMediaException.class, () -> selfdataDatasetApisHelper.getTpbcData(parameters));
	}

	private static Stream<Arguments> getMissingApiGatewayResponses() {
		return Stream.of(null, Arguments.of(new Api()));
	}

	@Test
	void test_getGdataData_500_when_searchApiErrorApiGateway() throws GetApiException {

		SelfdataApiParameters parameters = createValidParameters();

		when(apiGatewayHelper.getApiById(any(), any())).thenThrow(GetApiException.class);
		assertThrows(AppServiceException.class, () -> selfdataDatasetApisHelper.getGdataData(parameters));
	}

	@Test
	void test_getTpbcData_500_when_searchApiErrorApiGateway() throws GetApiException {

		SelfdataApiParameters parameters = createValidParameters();

		when(apiGatewayHelper.getApiById(any(), any())).thenThrow(GetApiException.class);
		assertThrows(AppServiceException.class, () -> selfdataDatasetApisHelper.getTpbcData(parameters));
	}

	@ParameterizedTest
	@MethodSource("getThrownExceptions")
	void test_getGdataData_500_when_technicalApiGatewayErrorCallingApiEndpoint(
			Class<? extends Exception> inputException) throws AppServiceNotFoundException, GetApiException {

		Api api = new Api();

		SelfdataApiParameters parameters = createValidParameters();

		when(apiGatewayHelper.getApiById(any(), any())).thenReturn(api);
		when(selfdataApiGatewayHelper.datasets(any(), any(), any())).thenThrow(inputException);
		assertThrows(AppServiceException.class, () -> selfdataDatasetApisHelper.getGdataData(parameters));
	}

	@ParameterizedTest
	@MethodSource("getThrownExceptions")
	void test_getTpbcData_500_when_technicalApigatewayErrorCallingApiEndpoint(Class<? extends Exception> inputException)
			throws AppServiceNotFoundException, GetApiException {

		Api api = new Api();

		SelfdataApiParameters parameters = createValidParameters();

		when(apiGatewayHelper.getApiById(any(), any())).thenReturn(api);
		when(selfdataApiGatewayHelper.datasets(any(), any(), any())).thenThrow(inputException);
		assertThrows(AppServiceException.class, () -> selfdataDatasetApisHelper.getTpbcData(parameters));
	}

	@Test
	void test_getGdataData_ok() throws AppServiceException {
		String mock;
		try {
			File file = new File("./src/test/resources/gdata-mocked-response.json");
			mock = FileUtils.readFileToString(file, "UTF-8");
		} catch (IOException e) {
			throw new AppServiceException("Erreur dans le chargement du mock", e);
		}

		ClientResponse successfulResponse = ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.getValue()).body(mock).build();

		Api api = new Api().methods(List.of(ApiMethod.GET));

		SelfdataApiParameters parameters = createValidParameters();

		when(apiGatewayHelper.getApiById(any(), any())).thenReturn(api);
		when(selfdataApiGatewayHelper.datasets(any(), any(), any())).thenReturn(successfulResponse);
		GenericDataObject gdata = selfdataDatasetApisHelper.getGdataData(parameters);

		assertNotNull(gdata);
		assertEquals("Informations sur les activités du producteur", gdata.getLegend().get(0).getText());
	}

	@ParameterizedTest
	@MethodSource("getValidDateParams")
	void test_getTpbcData_ok(OffsetDateTime minimum, OffsetDateTime maximum) throws AppServiceException {

		String mock;
		try {
			File file = new File("./src/test/resources/tpbc-mocked-response.json");
			mock = FileUtils.readFileToString(file, "UTF-8");
		} catch (IOException e) {
			throw new AppServiceException("Erreur dans le chargement du mock", e);
		}

		ClientResponse successfulResponse = ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.getValue()).body(mock).build();
		Api api = new Api().methods(List.of(ApiMethod.GET));

		SelfdataApiParameters parameters = createValidParameters();
		parameters.setMinDate(minimum);
		parameters.setMaxDate(maximum);

		when(apiGatewayHelper.getApiById(any(), any())).thenReturn(api);
		when(selfdataApiGatewayHelper.datasets(any(), any(), any())).thenReturn(successfulResponse);
		BarChartData tpbc = selfdataDatasetApisHelper.getTpbcData(parameters);

		assertNotNull(tpbc);
		assertEquals("Catégorie de bac", tpbc.getLegend().get(0).getText());
	}

	private static Stream<Arguments> getValidDateParams() {
		OffsetDateTime minimum = OffsetDateTime.parse("2020-01-01T15:20:30+08:00");
		OffsetDateTime maximum = OffsetDateTime.parse("2021-01-01T15:20:30+08:00");
		return Stream.of(Arguments.of(null, null), Arguments.of(minimum, null), Arguments.of(null, maximum),
				Arguments.of(minimum, maximum));
	}

	@Test
	void test_getTpbc_400_when_invalidDateParams() {

		OffsetDateTime minimum = OffsetDateTime.parse("2020-01-01T15:20:30+08:00");
		OffsetDateTime maximum = OffsetDateTime.parse("2021-01-01T15:20:30+08:00");

		SelfdataApiParameters parameters = createValidParameters();
		parameters.setMinDate(maximum);
		parameters.setMaxDate(minimum);

		assertThrows(AppServiceBadRequestException.class, () -> selfdataDatasetApisHelper.getTpbcData(parameters));
	}

	private SelfdataApiParameters createValidParameters() {
		SelfdataApiParameters parameters = new SelfdataApiParameters();

		AuthenticatedUser user = createValidUser();

		parameters.setMetadata(getValidMetadata());
		parameters.setUser(user);
		return parameters;
	}

	private AuthenticatedUser createValidUser() {
		AuthenticatedUser user = new AuthenticatedUser();
		user.setLogin("validUser@com");
		return user;
	}

	private static Stream<Arguments> getThrownExceptions() {
		return Stream.of(Arguments.of(IllegalArgumentException.class), Arguments.of(MissingApiForMediaException.class));
	}

	private static Metadata getSelfdataMetadata() {
		Metadata metadata = new Metadata().accessCondition(new MetadataAccessCondition()
				.confidentiality(new MetadataAccessConditionConfidentiality().gdprSensitive(true)));

		MetadataExtMetadataExtSelfdata extSelfdata = new MetadataExtMetadataExtSelfdata();
		extSelfdata.setExtSelfdataContent(new SelfdataContent());
		MetadataExtMetadata ext = new MetadataExtMetadata().extSelfdata(extSelfdata);
		metadata.setExtMetadata(ext);
		return metadata;
	}

	private static Metadata getMetadataWithoutApis() {
		Metadata metadata = getSelfdataMetadata();
		metadata.getExtMetadata().getExtSelfdata().getExtSelfdataContent()
				.setSelfdataAccess(SelfdataContent.SelfdataAccessEnum.API);
		return metadata;
	}

	private static Metadata getMetadataWithWrongApis() {
		Metadata metadata = getMetadataWithoutApis();
		List<Media> medias = new ArrayList<>();
		Media media1 = createMediaService("test1", InterfaceContract.DOWNLOAD, "http://chose.com");
		Media media2 = createMediaService("test2", InterfaceContract.WFS, "http://chose23.com");
		medias.add(media1);
		medias.add(media2);
		metadata.setAvailableFormats(medias);
		return metadata;
	}

	private static Metadata getMetadataWithTooMuchTpbc() {
		Metadata metadata = getMetadataWithoutApis();
		List<Media> medias = new ArrayList<>();
		Media media1 = createMediaService("test1", InterfaceContract.TEMPORAL_BAR_CHART, "http://chose.com");
		Media media2 = createMediaService("test2", InterfaceContract.TEMPORAL_BAR_CHART, "http://chose23.com");
		Media media3 = createMediaService("test3", InterfaceContract.GENERIC_DATA, "http://chose23.com");
		medias.add(media1);
		medias.add(media2);
		medias.add(media3);
		metadata.setAvailableFormats(medias);
		return metadata;
	}

	private static Metadata getMetadataWithTooMuchGdata() {
		Metadata metadata = getMetadataWithoutApis();
		List<Media> medias = new ArrayList<>();
		Media media1 = createMediaService("test1", InterfaceContract.TEMPORAL_BAR_CHART, "http://chose.com");
		Media media2 = createMediaService("test2", InterfaceContract.GENERIC_DATA, "http://chose23.com");
		Media media3 = createMediaService("test3", InterfaceContract.GENERIC_DATA, "http://chose23.com");
		medias.add(media1);
		medias.add(media2);
		medias.add(media3);
		metadata.setAvailableFormats(medias);
		return metadata;
	}

	private static Metadata getValidMetadata() {
		Metadata metadata = getMetadataWithoutApis();
		List<Media> medias = new ArrayList<>();
		Media media1 = createMediaService("test1", InterfaceContract.TEMPORAL_BAR_CHART, "http://chose.com");
		Media media2 = createMediaService("test2", InterfaceContract.GENERIC_DATA, "http://chose23.com");
		medias.add(media1);
		medias.add(media2);
		metadata.setAvailableFormats(medias);
		return metadata;
	}

	private static Media createMediaService(String name, InterfaceContract contract, String url) {
		return new Media().mediaName(name).mediaType(Media.MediaTypeEnum.SERVICE)
				.connector(new Connector().interfaceContract(contract.getCode()).url(url));
	}
}
