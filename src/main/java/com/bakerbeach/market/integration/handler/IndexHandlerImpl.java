package com.bakerbeach.market.integration.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bakerbeach.market.index.model.IndexContext;
import com.bakerbeach.market.index.service.XIndexService;
import com.bakerbeach.market.integration.model.ContextMap;
import com.bakerbeach.market.xcatalog.model.Product;
import com.bakerbeach.market.xcatalog.service.XCatalogService;

public class IndexHandlerImpl extends AbstractHandler implements AggregationStrategy {
	protected static final Logger log = LoggerFactory.getLogger(IndexHandlerImpl.class);
	private static final Integer DEFAULT_GTIN_CHUNK_SIZE = 20;
	private static Integer chunkSize = DEFAULT_GTIN_CHUNK_SIZE;

	@Autowired
	protected ContextMap contextMap;
	
	@Autowired
	protected XCatalogService catalogService;
	
	@Autowired
	private XIndexService indexService;

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> updateAll(Exchange ex) {
		try {
			Map<String, Object> payload = (Map<String, Object>) getPayload(ex.getIn());
			String shop = (String) payload.get("shop");
			Product.Status status = Product.Status.valueOf((String) payload.get("status"));
			Date lastUpdate = (payload.containsKey("lastUpdate")) ? (Date) payload.get("lastUpdate") : new Date();

			List<String> codes = catalogService.productCodes(shop, null, Arrays.asList(status), null, null, "code");

			List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
			for (Iterator<String> i = codes.iterator(); i.hasNext();) {
				List<String> _codes = new ArrayList<String>();

				for (int j = 0; j < chunkSize && i.hasNext(); j++) {
					_codes.add(i.next());
				}

				Map<String, Object> body = new HashMap<String, Object>(4);
				body.put("shop", shop);
				body.put("status", status.name());
				body.put("lastUpdate", lastUpdate);
				body.put("codes", _codes);

				out.add(body);
			}

			return out;
		} catch (EventHandlerException e) {
			log.error(ExceptionUtils.getStackTrace(e));
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public void index(Exchange ex) {
		try {
			log.info("index : " + ex.getIn().getBody());

			Map<String, Object> payload = (Map<String, Object>) getPayload(ex.getIn());
			String shop = (String) payload.get("shop");
			Product.Status status = Product.Status.valueOf((String) payload.get("status"));
			Date lastUpdate = (payload.containsKey("lastUpdate")) ? (Date) payload.get("lastUpdate") : new Date();
			List<String> codes = (List<String>) payload.get("codes");

			List<Product> products = catalogService.rawByGtin(shop, status, codes);
			IndexContext context = contextMap.get(shop);
			indexService.index(products, status, lastUpdate, context);
			
			System.out.println("test");

		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
	}

	/*
	 * @SuppressWarnings("unchecked") public void index(Exchange ex) { try {
	 * log.info("index : " + ex.getIn().getBody());
	 * 
	 * Map<String, Object> payload = (Map<String, Object>)
	 * getPayload(ex.getIn()); String shop = (String) payload.get("shop");
	 * String status = (String) payload.get("status"); Date lastUpdate =
	 * (payload.containsKey("lastUpdate")) ? (Date) payload.get("lastUpdate") :
	 * new Date(); List<String> gtin = (List<String>) payload.get("gtin");
	 * 
	 * IntegrationContext context = contextMap.get(shop); List<Locale> locales =
	 * context.getLocales(); List<Currency> currencies =
	 * context.getCurrencies(); List<String> priceGroups =
	 * context.getPriceGroups();
	 * 
	 * List<RawProduct> products = catalogService.findRawByGtin(status, gtin);
	 * 
	 * indexService.index(products, shop, status, lastUpdate, locales,
	 * currencies, priceGroups); } catch (Exception e) {
	 * log.error(ExceptionUtils.getStackTrace(e)); } }
	 */

	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		log.info("aggregate : " + newExchange.getIn().getBody());
		return newExchange;
	}

	public void afterUpdate(Exchange ex) {
		try {
			log.info("afterUpdate : " + ex.getIn().getBody());
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
	}

	public static void setGtinChunkSize(Integer gtinChunkSize) {
		IndexHandlerImpl.chunkSize = gtinChunkSize;
	}

}
