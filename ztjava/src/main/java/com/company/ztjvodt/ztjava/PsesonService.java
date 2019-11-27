package com.company.ztjvodt.ztjava;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.company.ztjvodt.ztjava.namespaces.demoservice2.Product;
import com.company.ztjvodt.ztjava.services.DefaultDemoService2Service;
import com.sap.cloud.sdk.odatav2.connectivity.ODataException;
import com.sap.cloud.sdk.odatav2.connectivity.ODataQueryBuilder;
import com.sap.cloud.sdk.odatav2.connectivity.ODataQueryResult;
import com.sap.cloud.sdk.service.prov.api.operations.Create;
import com.sap.cloud.sdk.service.prov.api.operations.Delete;
import com.sap.cloud.sdk.service.prov.api.operations.Query;
import com.sap.cloud.sdk.service.prov.api.operations.Read;
import com.sap.cloud.sdk.service.prov.api.operations.Update;
import com.sap.cloud.sdk.service.prov.api.request.CreateRequest;
import com.sap.cloud.sdk.service.prov.api.request.DeleteRequest;
import com.sap.cloud.sdk.service.prov.api.request.QueryRequest;
import com.sap.cloud.sdk.service.prov.api.request.ReadRequest;
import com.sap.cloud.sdk.service.prov.api.request.UpdateRequest;
import com.sap.cloud.sdk.service.prov.api.response.CreateResponse;
import com.sap.cloud.sdk.service.prov.api.response.DeleteResponse;
import com.sap.cloud.sdk.service.prov.api.response.ErrorResponse;
import com.sap.cloud.sdk.service.prov.api.response.QueryResponse;
import com.sap.cloud.sdk.service.prov.api.response.ReadResponse;
import com.sap.cloud.sdk.service.prov.api.response.UpdateResponse;

public class PsesonService {
	// private static final Logger LOGGER =
	// LoggerFactory.getLogger(PsesonService.class);

	// dummy database
	private static List<Map<String, Object>> peopleList = null;

	@Query(serviceName = "DemoService2", entity = "Products")
	public QueryResponse getProducts(QueryRequest request) {

		try {
			final List<Product> productsList = new DefaultDemoService2Service().getAllProduct()
					.select(Product.ID, Product.NAME, Product.DESCRIPTION).execute();
			return QueryResponse.setSuccess().setData(productsList).response();

		} catch (ODataException e) {
			return QueryResponse.setError(ErrorResponse.getBuilder()
					.setMessage("Error occurred while getting products from backend. See error log for details.")
					.setStatusCode(500).response());
		}
	}

	// @Query(serviceName = "DemoService2", entity = "Products")
	// public QueryResponse getProducts(QueryRequest request) {
	//
	// try {
	// ODataQueryResult result =
	// ODataQueryBuilder.withEntity("/V2/OData/OData.svc", "Products").build()
	// .execute("odatarefservices");
	// List<Map<String, Object>> productList = result.asListOfMaps();
	//
	// return QueryResponse.setSuccess().setDataAsMap(productList).response();
	//
	// } catch (ODataException e) {
	// return QueryResponse.setError(ErrorResponse.getBuilder()
	// .setMessage("Error occurred while getting products from backend. See
	// error log for details.")
	// .setStatusCode(500).response());
	// }
	// }

	@Query(serviceName = "DemoService", entity = "People")
	public QueryResponse getPeople(QueryRequest request) {

		List<Map<String, Object>> productList = new ArrayList<>();

		List<Map<String, Object>> resultList = null;
		try {
			ODataQueryResult result = ODataQueryBuilder.withEntity("/sap/opu/odata/sap/Z_DEMO_SRV", "EtPersonSet")
					.build().execute("zGatewayServers");
			resultList = result.asListOfMaps();
		} catch (ODataException e) {
			ErrorResponse response = ErrorResponse.getBuilder().setMessage("zerror:" + e.getMessage())
					.setStatusCode(500).response();
			return QueryResponse.setError(response);
		}

		for (Map<String, Object> resultMap : resultList) {
			Map<String, Object> personMap = new HashMap<String, Object>();
			personMap.put("UniqueId", Integer.parseInt((String) resultMap.get("Zid")));
			personMap.put("Name", "Zname");
			productList.add(personMap);
		}

		// productList = getPeople();

		return QueryResponse.setSuccess().setDataAsMap(productList).response();
	}

	@Read(serviceName = "DemoService", entity = "People")
	public ReadResponse getPerson(ReadRequest request) {

		Map<String, Object> keyPredicates = request.getKeys();
		Integer id = (Integer) keyPredicates.get("UniqueId");

		List<Map<String, Object>> peopleList = getPeople();

		Map<String, Object> requestedPersonMap = new HashMap<String, Object>();
		for (Map<String, Object> personMap : peopleList) {
			if (((Integer) personMap.get("UniqueId")).equals(id)) {
				// found it
				requestedPersonMap = personMap;
			}
		}
		if (requestedPersonMap.isEmpty()) {
			ErrorResponse response = ErrorResponse.getBuilder()
					.setMessage("Person with UniqueId " + id + " doesn't exist!").setStatusCode(404).response();
			return ReadResponse.setError(response);
		}

		return ReadResponse.setSuccess().setData(requestedPersonMap).response();
	}

	@Create(serviceName = "DemoService", entity = "People")
	public CreateResponse createPerson(CreateRequest request) {

		// extract the data for creation
		Map<String, Object> requestBody = request.getMapData();
		Integer idToCreate = (Integer) requestBody.get("UniqueId");
		String nameToCreate = (String) requestBody.get("Name");

		// do the actual creation in database
		createPerson(idToCreate, nameToCreate);

		return CreateResponse.setSuccess().response();
	}

	@Update(serviceName = "DemoService", entity = "People")
	public UpdateResponse updatePerson(UpdateRequest request) {

		// retrieve from request: which person to update
		Integer idForUpdate = (Integer) request.getKeys().get("UniqueId");

		// retrieve from request: the data to modify
		Map<String, Object> requestBody = request.getMapData();

		// retrieve from database: the person to modify
		Map<String, Object> personMap = findPerson(idForUpdate);

		// do the modification, but ignore key property (cannot be modified, to
		// keep data consistent)
		personMap.replace("Name", requestBody.get("Name"));

		return UpdateResponse.setSuccess().response();
	}

	@Delete(serviceName = "DemoService", entity = "People")
	public DeleteResponse deletePerson(DeleteRequest request) {
		Integer id = (Integer) request.getKeys().get("UniqueId");

		// find and remove the requested person
		List<Map<String, Object>> peopleList = getPeople();
		for (Iterator<Map<String, Object>> iterator = peopleList.iterator(); iterator.hasNext();) {
			Map<String, Object> personMap = iterator.next();
			if (((Integer) personMap.get("UniqueId")).equals(id)) {
				iterator.remove();
				break;
			}
		}

		return DeleteResponse.setSuccess().response();
	}

	/* Dummy Data Store */

	private List<Map<String, Object>> getPeople() {

		// init the "database"
		if (peopleList == null) {
			peopleList = new ArrayList<Map<String, Object>>();
			createPerson(0, "Anna");
			createPerson(1, "Berta");
			createPerson(2, "Claudia");
			createPerson(3, "Debbie");
		}

		return peopleList;
	}

	private void createPerson(Integer id, String name) {
		Map<String, Object> personMap = new HashMap<String, Object>();
		personMap.put("UniqueId", id);
		personMap.put("Name", name);

		peopleList.add(personMap);
	}

	private Map<String, Object> findPerson(Integer requiredPersonId) {
		List<Map<String, Object>> peopleList = getPeople();
		for (Map<String, Object> personMap : peopleList) {
			if (((Integer) personMap.get("UniqueId")).equals(requiredPersonId)) {
				return personMap;
			}
		}
		return null;
	}
}