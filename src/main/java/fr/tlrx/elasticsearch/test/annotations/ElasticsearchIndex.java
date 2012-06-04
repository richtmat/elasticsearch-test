/**
 * 
 */
package fr.tlrx.elasticsearch.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ElasticsearchIndex Annotation
 * 
 * @author tlrx
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ElasticsearchIndex {

	/**
	 * Default index name
	 */
	public static final String DEFAULT_NAME = "test";
	
	/**
	 * The index's name, default to "test"
	 */
	String indexName() default DEFAULT_NAME;	

	/**
	 * The node's name from which a client is instantiated to run operations
	 */
	String nodeName() default ElasticsearchNode.DEFAULT_NODE_NAME;
	
	/**
	 * Mappings of the index
	 */
	ElasticsearchMapping[] mappings() default {};
	
	/**
	 * Index settings
	 */
	ElasticsearchSetting[] settings() default {};	
	
	/**
	 * Analysis definitions
	 */
	ElasticsearchAnalysis analysis() default @ElasticsearchAnalysis();
}
