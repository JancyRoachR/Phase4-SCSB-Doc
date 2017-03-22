package org.recap.util;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCase;
import org.recap.service.accession.SolrIndexService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * Created by angelind on 6/2/17.
 */
public class OngoingMatchingAlgorithmUtilUT extends BaseTestCase{

    @Mock
    OngoingMatchingAlgorithmUtil ongoingMatchingAlgorithmUtil;

    @Test
    public void processMatchingForBibTest() {
        SolrDocument solrDocument = new SolrDocument();
        Mockito.when(ongoingMatchingAlgorithmUtil.processMatchingForBib(solrDocument)).thenReturn("Success");
        String status = ongoingMatchingAlgorithmUtil.processMatchingForBib(solrDocument);
        assertEquals("Success", status);
    }

}