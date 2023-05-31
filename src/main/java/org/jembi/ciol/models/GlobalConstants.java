package org.jembi.ciol.models;

public final class GlobalConstants {

        private GlobalConstants() {}
        public static final String TOPIC_NOTIFICATIONS = "CIOL-notifications";
        public static final String TOPIC_PAYLOAD_QUEUE = "CIOL-payload";
        public static final String METADATA_FILE_PATH = "/app/conf/MetadataConfigURL.json";

        public static final String SAMPLE_METADATA_CONFIG_FILE =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleConfigFileValid.json";
        public static final String SAMPLE_REPORT_DATA_VALID =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleReportDataValid.json";
        public static final String SAMPLE_REPORT_DATA_INVALID_MV =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleReportDataInvalidMV.json";
        public static final String SAMPLE_REPORT_DATA_INVALID_OUI =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleReportDataInvalidOrgUnitId.json";
        public static final String SAMPLE_REPORT_DATA_INVALID_PER =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleReportDataInvalidPeriod.json";
        public static final String SAMPLE_REPORT_DATA_INVALID_DISAGG_SINGLE =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleReportDataInvalidDisaggSingle.json";
        public static final String SAMPLE_REPORT_DATA_INVALID_DISAGG_MULTI =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleReportDataInvalidDisaggMulti.json";

        public static final String SAMPLE_REPORT_DATA_FULL =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleReportDataFull.json";

        public static final String SAMPLE_CONFIG_FILE_FULL =
                "src/main/java/org/jembi/ciol/Test/SampleData/sampleConfigFileFull.json";
}