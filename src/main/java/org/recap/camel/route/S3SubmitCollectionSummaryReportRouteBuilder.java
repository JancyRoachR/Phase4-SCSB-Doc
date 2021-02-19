package org.recap.camel.route;

import org.apache.camel.CamelContext;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.model.dataformat.BindyType;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.model.csv.SubmitCollectionReportRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by hemalathas on 21/12/16.
 */
@Component
public class S3SubmitCollectionSummaryReportRouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(S3SubmitCollectionSummaryReportRouteBuilder.class);

    Predicate checkForProtectionOrNotProtectionKeyword = exchange -> {
        if (exchange.getIn().getHeader("fileName") != null) {
            String fileName = (String) exchange.getIn().getHeader("fileName");
            boolean flag = fileName.contains(RecapConstants.PROTECTED) || fileName.contains(RecapConstants.NOT_PROTECTED);
            return flag;
        } else {
            return false;
        }
    };

    /**
     * This method instantiates a new route builder to generate submit collection summary report to the S3.
     *
     * @param context the context
     */
    @Autowired
    public S3SubmitCollectionSummaryReportRouteBuilder(CamelContext context, @Value("${s3.add.s3.routes.on.startup}") boolean addS3RoutesOnStartup) {
        try {
            if (addS3RoutesOnStartup) {
                context.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from(RecapConstants.FTP_SUBMIT_COLLECTION_SUMMARY_REPORT_Q)
                                .routeId(RecapConstants.FTP_SUBMIT_COLLECTION_SUMMARY_REPORT_ID)
                                .marshal().bindy(BindyType.Csv, SubmitCollectionReportRecord.class)
                                .choice()
                                .when(checkForProtectionOrNotProtectionKeyword)
                                .setHeader(S3Constants.KEY, simple(RecapConstants.SUBMIT_COLLECTION_REPORTS_BASE_PATH + "${in.header.fileName}"))
                                .to(RecapConstants.SCSB_CAMEL_S3_TO_ENDPOINT)
                                .otherwise()
                                .setHeader(S3Constants.KEY, simple(RecapConstants.SUBMIT_COLLECTION_MANUAL_REPORTS_BASE_PATH + "${in.header.fileName}"))
                                .to(RecapConstants.SCSB_CAMEL_S3_TO_ENDPOINT)
                                .endChoice();
                    }
                });
            }
        } catch (Exception e) {
            logger.error(RecapCommonConstants.LOG_ERROR, e);
        }
    }

}
