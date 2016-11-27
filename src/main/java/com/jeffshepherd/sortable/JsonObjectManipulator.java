/**
 * 
 */
package com.jeffshepherd.sortable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

/**
 * Base class which manipulates JSON objects.
 * @author Jeff Shepherd
 */
abstract class JsonObjectManipulator {
	private final ObjectMapper mapper = new ObjectMapper();
	
	public JsonObjectManipulator() {
		mapper.registerModule(new JsonOrgModule());
	}
	
	public ObjectMapper getMapper() {
		return mapper;
	}
}
