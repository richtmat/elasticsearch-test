package fr.tlrx.elasticsearch.samples.indices;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.elasticsearch.action.admin.indices.status.DocsStatus;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.node.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.tlrx.elasticsearch.test.annotations.ElasticsearchAdminClient;
import fr.tlrx.elasticsearch.test.annotations.ElasticsearchClient;
import fr.tlrx.elasticsearch.test.annotations.ElasticsearchNode;
import fr.tlrx.elasticsearch.test.support.junit.runners.ElasticsearchRunner;

/**
 * Test Java API / Indices : Optimize
 * 
 * @author tlrx
 * 
 */
@RunWith(ElasticsearchRunner.class)
public class OptimizeTest {

	@ElasticsearchNode
	Node node0;

	@ElasticsearchClient
	Client client;

	@ElasticsearchAdminClient
	AdminClient admin;
	
	private static int NB = 500;
	private static String INDEX = "my_index";
	private static String TYPE = "my_type";
	private static int deleted = 0;
	
	@Before
	public void setUp() throws IOException {

		// Creates NB documents
		BulkRequestBuilder bulkRequestBuilder = new BulkRequestBuilder(client);
		
		for (int i = 0; i < NB; i++) {
			IndexRequest indexRequest = new IndexRequest(INDEX)
												.type(TYPE)
												.id(String.valueOf(i))
												.source(JsonXContent.contentBuilder()
																		.startObject()
																			.field("title", "Object #" + i)
																		.endObject()
														);
			bulkRequestBuilder.add(indexRequest);
		}
		
		BulkResponse bulkResponse = bulkRequestBuilder.setRefresh(true).execute().actionGet();
		System.out.printf("Bulk request executed in %d ms, %d document(s) indexed, failures : %s.\r\n", bulkResponse.tookInMillis(), NB, bulkResponse.hasFailures());
		
		// Deletes some documents
		for (int i = 0; i < NB; i = i + 9) {
			DeleteResponse deleteResponse = client
					.prepareDelete(INDEX, TYPE, String.valueOf(i))
					.setRefresh(true)
					.execute()
					.actionGet();

			if (deleteResponse.notFound()) {
				System.out.printf("Unable to delete document [id:%d], not found.\r\n", i);
			} else {
				deleted++;
				System.out.printf("Document [id:%d] deleted.\r\n", i);
			}
		}
		System.out.printf("%d document(s) deleted.\r\n", deleted);
	}
	
	@Test
	public void testOptimize() {
		
		// Count documents number
		CountResponse countResponse = client.prepareCount(INDEX).setTypes(TYPE).execute().actionGet();
		assertEquals((NB - deleted), countResponse.count());
		
		// Retrieves document status for the index
		IndicesStatusResponse status = admin.indices().prepareStatus(INDEX).execute().actionGet();
		DocsStatus docsStatus = status.index(INDEX).docs();
		
		// Check docs status
		System.out.printf("DocsStatus before optimize: %d numDocs, %d maxDocs, %d deletedDocs\r\n", docsStatus.getNumDocs(), docsStatus.getMaxDoc(), docsStatus.getDeletedDocs());
		assertEquals((NB - deleted), docsStatus.getNumDocs());
		assertEquals(NB, docsStatus.getMaxDoc());
		assertEquals(deleted, docsStatus.getDeletedDocs());
		
		// Now optimize the index
		admin.indices().prepareOptimize(INDEX)
				.setFlush(true)
				.setOnlyExpungeDeletes(true)
				.setWaitForMerge(true)
				.execute()
				.actionGet();

		// Retrieves document status gain
		docsStatus = admin.indices().prepareStatus(INDEX).execute().actionGet().index(INDEX).docs();
		
		// Check again docs status
		System.out.printf("DocsStatus after optimize: %d numDocs, %d maxDocs, %d deletedDocs\r\n", docsStatus.getNumDocs(), docsStatus.getMaxDoc(), docsStatus.getDeletedDocs());
		assertEquals((NB - deleted), docsStatus.getNumDocs());
		assertEquals((NB - deleted), docsStatus.getMaxDoc());
		// Must be zero
		assertEquals(0, docsStatus.getDeletedDocs());		
	}
}
