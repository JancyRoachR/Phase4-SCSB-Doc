package org.recap.executors;

import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsQueueEndpoint;
import org.apache.camel.component.solr.SolrConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.recap.BaseTestCase;
import org.recap.BaseTestCaseUT;
import org.recap.RecapCommonConstants;
import org.recap.admin.SolrAdmin;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.solr.SolrIndexRequest;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.recap.repository.jpa.HoldingsDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.solr.main.BibSolrCrudRepository;
import org.recap.repository.solr.temp.BibCrudRepositoryMultiCoreSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Created by premkb on 29/7/16.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SolrTemplate.class)
@PowerMockRunnerDelegate(SpringJUnit4ClassRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
public class BibItemIndexExecutorServiceUT extends BaseTestCaseUT {

    private static final Logger logger = LoggerFactory.getLogger(BibItemIndexExecutorServiceUT.class);

    @InjectMocks
    BibItemIndexExecutorService bibItemIndexExecutorService;

    @Mock
    BibliographicDetailsRepository mockBibliographicDetailsRepository;

    @Mock
    SolrAdmin mockSolrAdmin;

    @Mock
    BibItemIndexCallable mockBibItemIndexCallable;

    @Mock
    BibCrudRepositoryMultiCoreSupport mockBibCrudRepositoryMultiCoreSupport;

    @Mock
    InstitutionDetailsRepository institutionDetailsRepository;

    @Mock
    BibSolrCrudRepository bibSolrCrudRepository;

    @Mock
    ProducerTemplate producerTemplate;

    @Mock
    SolrTemplate solrTemplate;

    @Mock
    HoldingsDetailsRepository holdingsDetailsRepository;

    @Mock
    CamelContext camelContext;

    @Value("${solr.router.uri.type}")
    String solrRouterURI;

    @Value("${solr.server.protocol}")
    String solrServerProtocol;

    @Value("${solr.url}")
    String solrUrl;

    @Value("${solr.parent.core}")
    String solrCore;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(bibItemIndexExecutorService,"solrServerProtocol",solrServerProtocol);
        ReflectionTestUtils.setField(bibItemIndexExecutorService,"solrUrl",solrUrl);
        ReflectionTestUtils.setField(bibItemIndexExecutorService,"solrCore",solrCore);
        ReflectionTestUtils.setField(bibItemIndexExecutorService,"solrRouterURI",solrRouterURI);
    }

    @Test
    public void mergeIndexFrequency() throws Exception {

        Mockito.when(mockBibliographicDetailsRepository.countByIsDeletedFalse()).thenReturn(500000L);
        Mockito.when(mockBibItemIndexCallable.call()).thenReturn(1000);

        bibItemIndexExecutorService.setBibliographicDetailsRepository(mockBibliographicDetailsRepository);
        bibItemIndexExecutorService.setSolrAdmin(mockSolrAdmin);
        SolrIndexRequest solrIndexRequest = new SolrIndexRequest();
        solrIndexRequest.setNumberOfThreads(5);
        solrIndexRequest.setNumberOfDocs(1);
        solrIndexRequest.setOwningInstitutionCode("PUL");
        solrIndexRequest.setCommitInterval(0);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(RecapCommonConstants.INCREMENTAL_DATE_FORMAT);
        Date from = DateUtils.addDays(new Date(), -1);
        solrIndexRequest.setDateFrom(dateFormatter.format(from));
        InstitutionEntity institutionEntity=new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionCode("PUL");
        institutionEntity.setInstitutionName("Princeton");
        Mockito.when(institutionDetailsRepository.findByInstitutionCode(Mockito.anyString())).thenReturn(institutionEntity);
        Mockito.when(mockBibliographicDetailsRepository.countByOwningInstitutionIdAndLastUpdatedDateAfter(Mockito.anyInt(),Mockito.any())).thenReturn(0l);
        Page bibliographicEntities = PowerMockito.mock(Page.class);
        SolrTemplate mocksolrTemplate1 = PowerMockito.mock(SolrTemplate.class);
        ReflectionTestUtils.setField(bibItemIndexExecutorService,"solrTemplate",mocksolrTemplate1);
        Iterator<BibliographicEntity> iterator=new Iterator<BibliographicEntity>() {
            int count;

            @Override
            public boolean hasNext() {
                count ++;
                if(count==1){
                    return true;}
                else {
                    return false;
                }
            }

            @SneakyThrows
            @Override
            public BibliographicEntity next() {
                return getBibliographicEntity();
            }
        };
        Mockito.when(bibliographicEntities.getNumberOfElements()).thenReturn(1);
        Mockito.when(bibliographicEntities.iterator()).thenReturn(iterator);
        Mockito.when(mockBibliographicDetailsRepository.findByOwningInstitutionIdAndLastUpdatedDateAfter(Mockito.any(),Mockito.anyInt(),Mockito.any())).thenReturn(bibliographicEntities);
        Mockito.when( bibSolrCrudRepository.countByDocType(Mockito.anyString())).thenReturn(1l);
        SolrInputDocument solrInputDocument=new SolrInputDocument();
        Mockito.when(mocksolrTemplate1.convertBeanToSolrInputDocument(Mockito.any())).thenReturn(solrInputDocument);
        Mockito.when(producerTemplate.getCamelContext()).thenReturn(camelContext);
        JmsQueueEndpoint jmsQueueEndpoint=Mockito.mock(JmsQueueEndpoint.class);
        Mockito.when(camelContext.getEndpoint(Mockito.anyString())).thenReturn(jmsQueueEndpoint);
        Mockito.when(jmsQueueEndpoint.getExchanges()).thenReturn(new ArrayList<>());
        CompletableFuture<Object> future=Mockito.mock(CompletableFuture.class);
        Mockito.when(producerTemplate.asyncRequestBodyAndHeader(solrRouterURI + "://" + solrUrl + "/" + solrCore, "", SolrConstants.OPERATION, SolrConstants.OPERATION_COMMIT)).thenReturn(future);
        Mockito.when(!future.isDone()).thenReturn(true);
        Mockito.when(future.get()).thenReturn(1);
        bibItemIndexExecutorService.index(solrIndexRequest);
    }

    private class MockBibItemIndexExecutorService extends BibItemIndexExecutorService {
        @Override
        public Callable getCallable(String coreName, int startingPage, int numRecordsPerPage, Integer owningInstitutionId, Date fromDate, String partialIndexType, Map<String, Object> partialIndexMap) {
            return mockBibItemIndexCallable;
        }

        @Override
        protected BibCrudRepositoryMultiCoreSupport getBibCrudRepositoryMultiCoreSupport(String solrUrl, String coreName) {
            return mockBibCrudRepositoryMultiCoreSupport;
        }
    }

    public BibliographicEntity getBibliographicEntity() throws URISyntaxException, IOException {
        File bibContentFile = getBibContentFile("BibContent.xml");
        String sourceBibContent = FileUtils.readFileToString(bibContentFile, "UTF-8");
        File holdingsContentFile = getBibContentFile("HoldingsContent.xml");
        String sourceHoldingsContent = FileUtils.readFileToString(holdingsContentFile, "UTF-8");

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent(sourceBibContent.getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId("1421");
        bibliographicEntity.setBibliographicId(1);
        List<BibliographicEntity> bibliographicEntitylist = new LinkedList(Arrays.asList(bibliographicEntity));


        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent(sourceHoldingsContent.getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionHoldingsId("1621");
        holdingsEntity.setHoldingsId(1);
        List<HoldingsEntity> holdingsEntitylist = new LinkedList(Arrays.asList(holdingsEntity));

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId("6320902");
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode("32101086866140");
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("PA");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setItemId(1);
        List<ItemEntity> itemEntitylist = new LinkedList(Arrays.asList(itemEntity));

        holdingsEntity.setBibliographicEntities(bibliographicEntitylist);
        holdingsEntity.setItemEntities(itemEntitylist);
        bibliographicEntity.setHoldingsEntities(holdingsEntitylist);
        bibliographicEntity.setItemEntities(itemEntitylist);
        itemEntity.setHoldingsEntities(holdingsEntitylist);
        itemEntity.setBibliographicEntities(bibliographicEntitylist);
        return bibliographicEntity;
    }


    public File getBibContentFile(String xml) throws URISyntaxException {
        URL resource = getClass().getResource(xml);
        return new File(resource.toURI());
    }
}
