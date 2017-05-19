package com.bakerbeach.market.integration.model;

import java.util.HashMap;
import java.util.Map;

import com.bakerbeach.market.index.model.IndexContext;

public class ContextMap extends HashMap<String, IndexContext> {
	private static final long serialVersionUID = 1L;

	public ContextMap(Map<? extends String, ? extends IndexContext> m) {
		super(m);
	}

}
