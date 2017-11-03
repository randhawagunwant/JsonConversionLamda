package org.collegeboard.dmf.essayscore.jsonconversion;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class JsonConversionHandler implements RequestHandler<JsonConversionRequest, JsonConversionHandlerResponse>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonConversionHandler.class);

    private static final String COL_DELIMITER = "|";
    private static final String ROW_DELIMITER = "|END";

    private static final String HEADER = "HD|";
    private static final String TRAILER = "TR|";
    private static final String ATOMIC_ELEMENT = "AS|";

    private static final String AS_SECTION_HEADER = "AS";
    private static final String SCORE_TIER_SECTION_HEADER = "ST";
    private static final String SCORE_READER_SECTION_HEADER = "RS";

    private static final int AS_SECTION_EXPECTED_SIZE = 21;
    private static final int SCORE_TIER_SECTION_EXPECTED_SIZE = 17;
    private static final int SCORE_READER_SECTION_EXPECTED_SIZE = 7;

    private static final String MESSAGE_TYPE_ATTR_NAME = "messageType";
    private static final String MESSAGE_TYPE_ID_ATTR_NAME = "messageTypeId";
    private static final String FILE_NAME_ATTR_NAME = "fileName";
    private static final String RECORD_NO_ATTR_NAME = "recordNo";
    private static final String TOTAL_RECORDS_ATTR_NAME = "totalRecords";
    private static final String SCORE_TIER_ATTR_NAME = "scoreTiers";
    private static final String SCORE_READER_ATTR_NAME = "scoreReaders";
    private static final String SCORE_READER_SEQUENCE_ATTR_NAME = "readerSequence";

    private static final int EXPECTED_SCORE_TIER_SECTIONS = 3;
    private static final int[] EXPECTED_SCORE_READER_SECTIONS = { 0, 2, 3 };

    private static final String JSON_OPEN_BRACE = "{";
    private static final String JSON_CLOSE_BRACE = "}";
    private static final String JSON_SEPERATOR = ",";
    private static final String JSON_ARRAY_OPEN_BRACKET = "[";
    private static final String JSON_ARRAY_CLOSE_BRACKET = "]";

    private static final int ATTRIBUTE_TYPE_STRING = 1;
    private static final int ATTRIBUTE_TYPE_NUMBER = 2;

    private static String snsArn;
    private static String sqsArn;

    private AmazonSQS sqsClient;
    private static Map<String, Map<String, AttributeDetails>> sectionAttributeDetails;

    static
    {
        sectionAttributeDetails = new LinkedHashMap<>();

        Map<String, AttributeDetails> attributeDetailsMap = new LinkedHashMap<>();
        attributeDetailsMap.put("uin", new AttributeDetails(1, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("actionIndicator", new AttributeDetails(3, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("scoreHoldIndicator", new AttributeDetails(4, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("scoreHoldType", new AttributeDetails(5, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("regNumber", new AttributeDetails(6, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("assessment", new AttributeDetails(7, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("adminYear", new AttributeDetails(8, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("adminMonth", new AttributeDetails(9, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("adminDay", new AttributeDetails(10, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("systemFormCode", new AttributeDetails(11, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("ssdFlag", new AttributeDetails(12, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("ssdType", new AttributeDetails(13, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("scoreKeyVersion", new AttributeDetails(14, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("asScanProcessedTimestamp", new AttributeDetails(15, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("naqMetadataMap", new AttributeDetails(16, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("numberOfASScanMetadata", new AttributeDetails(17, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("numberOfNonASQuestions", new AttributeDetails(18, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("numberOfSections", new AttributeDetails(19, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("numberOfScoreTiers", new AttributeDetails(20, ATTRIBUTE_TYPE_NUMBER, false));
        sectionAttributeDetails.put(AS_SECTION_HEADER, attributeDetailsMap);

        attributeDetailsMap = new LinkedHashMap<>();
        attributeDetailsMap.put("scoreName", new AttributeDetails(2, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("scoreValidIndicator", new AttributeDetails(4, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("score", new AttributeDetails(5, ATTRIBUTE_TYPE_NUMBER, false));
        attributeDetailsMap.put("conversionMapVersion", new AttributeDetails(6, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("scoreTimeStamp", new AttributeDetails(7, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("scoreMethodCode", new AttributeDetails(8, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("rightsCount", new AttributeDetails(9, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("wrongsCount", new AttributeDetails(10, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("omitsCount", new AttributeDetails(11, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("multiGriddedCount", new AttributeDetails(12, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("notReachedCount", new AttributeDetails(13, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("essayFormCode", new AttributeDetails(14, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("essayConditionCode", new AttributeDetails(15, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("numberOfEssayReaders", new AttributeDetails(16, ATTRIBUTE_TYPE_NUMBER, false));
        sectionAttributeDetails.put(SCORE_TIER_SECTION_HEADER, attributeDetailsMap);

        attributeDetailsMap = new LinkedHashMap<>();
        attributeDetailsMap.put("readerId", new AttributeDetails(3, ATTRIBUTE_TYPE_STRING, true));
        attributeDetailsMap.put("readerScore", new AttributeDetails(4, ATTRIBUTE_TYPE_NUMBER, false));
        attributeDetailsMap.put("readerScoreVersion", new AttributeDetails(5, ATTRIBUTE_TYPE_STRING, false));
        attributeDetailsMap.put("readerScoreDate", new AttributeDetails(6, ATTRIBUTE_TYPE_STRING, false));
        sectionAttributeDetails.put(SCORE_READER_SECTION_HEADER, attributeDetailsMap);

        snsArn = System.getenv("DMF_ESSAYSCORING_INPUT_TOPIC_ARN");
        sqsArn = System.getenv("DMF_ESSAYSCORING_INPUT_ERRORS_QUEUE_ARN");
    }

    private AmazonSNS snsClient;

    private AmazonS3 s3Client;

    public JsonConversionHandler()
    {
        s3Client = AmazonS3ClientBuilder.standard().build();
        snsClient = AmazonSNSClientBuilder.standard().build();
        sqsClient = AmazonSQSClientBuilder.standard().build();
    }

    @Override
    public JsonConversionHandlerResponse handleRequest(JsonConversionRequest request, Context context)
    {
        LOGGER.info(
                "Start Json Conversion Handler. startAtomicSegmentIndex:{},endAtomicSegmentIndex:{},FileName:{},totalAtomicSegmentCount:{}",
                request.getStartAtomicSegmentIndex(), request.getEndAtomicSegmentIndex(), request.getFileName(), request
                        .getTotalAtomicSegmentCount());
        JsonConversionHandlerResponse response = new JsonConversionHandlerResponse();
        try
        {
            processEssayScoreFile(request);

            response.setStatus("SUCCESS");
            LOGGER.info(
                    "End Json Conversion Handler. startAtomicSegmentIndex:{},endAtomicSegmentIndex:{},FileName:{},totalAtomicSegmentCount:{}",
                    request.getStartAtomicSegmentIndex(), request.getEndAtomicSegmentIndex(), request.getFileName(),
                    request.getTotalAtomicSegmentCount());
        } catch (Exception ex)
        {
            LOGGER.error(ex.getMessage(), ex);
            response.setStatus("ERROR");
        }
        return response;
    }

    private void processEssayScoreFile(JsonConversionRequest request)
    {
        S3Object s3Object = null;
        DelimitedFileReader in = null;
        try
        {
            s3Object = s3Client.getObject(request.getBucketName(), request.getFileName());
            in = new DelimitedFileReader(s3Object.getObjectContent(), ATOMIC_ELEMENT, HEADER, TRAILER, ROW_DELIMITER);

            LinkedList<DelimitedFileRecord> atomicSegment = null;
            in.seek(request.getStartAtomicSegmentIndex());
            int index = 0;
            for (long i = request.getStartAtomicSegmentIndex(); i <= request.getEndAtomicSegmentIndex(); i++)
            {
                atomicSegment = in.readAtomicDataSegment();
                processAtomicSegment(atomicSegment, request.getFileName(), i, request.getTotalAtomicSegmentCount());
                index++;
            }
            LOGGER.info("processed count::::::::::::::::::::::::::::::::::::::::::" + index);
        } catch (AmazonServiceException ase)
        {
            LOGGER.error(ase.getMessage(), ase);
            String message = String.format(
                    "S3 rejected:Error Message:%s,HTTP Status Code:%s,AWS Error Code:%s,Error Type:%s,Request ID:%s",
                    ase.getMessage(), ase.getStatusCode(), ase.getErrorCode(), ase.getErrorType(), ase.getRequestId());

            throw new RuntimeException(message, ase);
        } catch (AmazonClientException ace)
        {
            LOGGER.error(ace.getMessage(), ace);
            throw new RuntimeException(ace);

        } catch (Exception ex)
        {
            LOGGER.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);

        } finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
                if (s3Object != null)
                {
                    s3Object.close();
                }
            } catch (Exception ex)
            {
                LOGGER.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }

    }

    private void processAtomicSegment(LinkedList<DelimitedFileRecord> atomicSegment, String keyName, long recordNo,
            long totalRecords)
    {
        try
        {
            StringBuilder jsonString = new StringBuilder();
            jsonString.append(JSON_OPEN_BRACE);

            addContextDetails(jsonString, keyName, recordNo, totalRecords);

            processASSection(atomicSegment, jsonString);

            jsonString.append(JSON_SEPERATOR).append("\"").append(SCORE_TIER_ATTR_NAME).append("\"").append(":").append(
                    JSON_ARRAY_OPEN_BRACKET);

            for (int i = 1; i <= EXPECTED_SCORE_TIER_SECTIONS; i++)
            {
                processScoreTierSection(atomicSegment, jsonString);

                if (i != EXPECTED_SCORE_TIER_SECTIONS)
                {
                    jsonString.append(JSON_SEPERATOR);
                }
            }
            jsonString.append(JSON_ARRAY_CLOSE_BRACKET);

            jsonString.append(JSON_CLOSE_BRACE);
            publishToSNS(jsonString.toString());
        } catch (Exception ex)
        {
            //TODO push to queue
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    private void addContextDetails(StringBuilder jsonString, String fileName, long recordNo, long totalRecords)
    {
        appendDataAttribute(jsonString, MESSAGE_TYPE_ATTR_NAME, "essayscore", ATTRIBUTE_TYPE_STRING);
        jsonString.append(JSON_SEPERATOR);
        appendDataAttribute(jsonString, MESSAGE_TYPE_ID_ATTR_NAME, "1", ATTRIBUTE_TYPE_STRING);
        jsonString.append(JSON_SEPERATOR);
        appendDataAttribute(jsonString, FILE_NAME_ATTR_NAME, fileName, ATTRIBUTE_TYPE_STRING);
        jsonString.append(JSON_SEPERATOR);
        appendDataAttribute(jsonString, RECORD_NO_ATTR_NAME, String.valueOf(recordNo), ATTRIBUTE_TYPE_NUMBER);
        jsonString.append(JSON_SEPERATOR);
        appendDataAttribute(jsonString, TOTAL_RECORDS_ATTR_NAME, String.valueOf(totalRecords), ATTRIBUTE_TYPE_NUMBER);
        jsonString.append(JSON_SEPERATOR);
    }

    private StringBuilder processASSection(LinkedList<DelimitedFileRecord> atomicSegment, StringBuilder jsonString)
    {
        DelimitedFileRecord fileRecord = atomicSegment.poll();

        String[] dataArray = getDataArray(fileRecord.getRecordData());

        if (validDataArray(dataArray, AS_SECTION_HEADER, AS_SECTION_EXPECTED_SIZE))
        {
            appendDataAttributes(sectionAttributeDetails.get(AS_SECTION_HEADER), jsonString, dataArray);
        } else
        {
            throw new RuntimeException("Invalid Data");
        }

        return jsonString;
    }

    private StringBuilder processScoreTierSection(LinkedList<DelimitedFileRecord> atomicSegment,
            StringBuilder jsonString)
    {

        DelimitedFileRecord fileRecord = atomicSegment.poll();

        String[] dataArray = getDataArray(fileRecord.getRecordData());

        if (validDataArray(dataArray, SCORE_TIER_SECTION_HEADER, SCORE_TIER_SECTION_EXPECTED_SIZE))
        {
            jsonString.append(JSON_OPEN_BRACE);

            appendDataAttributes(sectionAttributeDetails.get(SCORE_TIER_SECTION_HEADER), jsonString, dataArray);

            jsonString.append(JSON_SEPERATOR).append("\"").append(SCORE_READER_ATTR_NAME).append("\"").append(":")
                    .append(JSON_ARRAY_OPEN_BRACKET);
            int sequence = 1;
            while (true)
            {
                processScoreReaderSection(atomicSegment, jsonString, sequence);

                if (!atomicSegment.isEmpty() && atomicSegment.getFirst().getRecordData().startsWith(
                        SCORE_READER_SECTION_HEADER))
                {
                    jsonString.append(JSON_SEPERATOR);
                    sequence++;
                } else
                {
                    break;
                }
            }

            if (!ArrayUtils.contains(EXPECTED_SCORE_READER_SECTIONS, sequence))
            {
                throw new RuntimeException("Invalid Data");
            }

            jsonString.append(JSON_ARRAY_CLOSE_BRACKET);
            jsonString.append(JSON_CLOSE_BRACE);

        } else
        {
            throw new RuntimeException("Invalid Data");
        }

        return jsonString;
    }

    private StringBuilder processScoreReaderSection(LinkedList<DelimitedFileRecord> atomicSegment,
            StringBuilder jsonString, int sequence)
    {

        DelimitedFileRecord fileRecord = atomicSegment.poll();

        String[] dataArray = getDataArray(fileRecord.getRecordData());

        if (validDataArray(dataArray, SCORE_READER_SECTION_HEADER, SCORE_READER_SECTION_EXPECTED_SIZE))
        {
            jsonString.append(JSON_OPEN_BRACE);
            appendDataAttribute(jsonString, SCORE_READER_SEQUENCE_ATTR_NAME, String.valueOf(sequence),
                    ATTRIBUTE_TYPE_NUMBER);
            jsonString.append(JSON_SEPERATOR);
            appendDataAttributes(sectionAttributeDetails.get(SCORE_READER_SECTION_HEADER), jsonString, dataArray);
            jsonString.append(JSON_CLOSE_BRACE);

        } else
        {
            throw new RuntimeException("Invalid Data");
        }

        return jsonString;
    }

    private void appendDataAttributes(Map<String, AttributeDetails> attributeDetailsMap, StringBuilder jsonString,
            String[] dataArray)
    {
        int max = attributeDetailsMap.size();
        int entityIndex = 1;

        AttributeDetails attributeDetails = null;
        String value = null;
        for (Map.Entry<String, AttributeDetails> entity : attributeDetailsMap.entrySet())
        {

            attributeDetails = entity.getValue();
            value = dataArray[attributeDetails.getPosition()];
            if (attributeDetails.isTrim() && value != null)
            {
                value = value.trim();
            }
            appendDataAttribute(jsonString, entity.getKey(), value, attributeDetails
                    .getType());
            if (entityIndex == max)
            {
                return;
            }
            jsonString.append(JSON_SEPERATOR);
            entityIndex++;
        }
    }

    private void appendDataAttribute(StringBuilder jsonString, String key, String value, int dataType)
    {
        if (dataType == ATTRIBUTE_TYPE_STRING)
        {
            jsonString.append("\"").append(key).append("\"").append(":").append("\"").append(value).append("\"");
        } else
        {
            jsonString.append("\"").append(key).append("\"").append(":").append(value);
        }
    }

    private boolean validDataArray(String[] dataArray, String sectionHeader, Integer sectoinExcpetionSize)
    {
        if (dataArray != null && dataArray.length == sectoinExcpetionSize && sectionHeader.equals(dataArray[0]))
        {
            return true;
        }
        return false;
    }

    private String[] getDataArray(String dataRecord)
    {
        return StringUtils.splitByWholeSeparatorPreserveAllTokens(dataRecord, COL_DELIMITER);
    }

    private void publishToSNS(String messageStr)
    {
        snsClient.publish(snsArn, messageStr);
    }

}
