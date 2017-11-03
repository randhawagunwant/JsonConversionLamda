package org.collegeboard.dmf.xml.jsonconversion;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class JsonConversionHandler implements RequestHandler<S3Event, JsonConversionHandlerResponse> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonConversionHandler.class);

	public static int PRETTY_PRINT_INDENT_FACTOR = 4;

	private static String snsArn;

	private static String sqsArn;

	private AmazonSQS sqsClient;

	private AmazonSNS snsClient;

	private AmazonS3 s3Client;

	static {
		snsArn = System.getenv("DMF_INPUT_TOPIC_ARN");
		sqsArn = System.getenv("DMF_INPUT_ERRORS_QUEUE_ARN");
	}

	public JsonConversionHandler() {
		s3Client = AmazonS3ClientBuilder.standard().build();
		snsClient = AmazonSNSClientBuilder.standard().build();
		sqsClient = AmazonSQSClientBuilder.standard().build();
	}

	@Override
	public JsonConversionHandlerResponse handleRequest(S3Event request, Context context) {
		LOGGER.info("Start Json Conversion Handler.");
		JsonConversionHandlerResponse response = new JsonConversionHandlerResponse();
		try {

			for (S3EventNotificationRecord record : request.getRecords()) {

				processXmlFile(record.getS3().getBucket().getName(), record.getS3().getObject().getKey());

			}

			response.setStatus("SUCCESS");
			LOGGER.info("End Json Conversion Handler.");
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			response.setStatus("ERROR");
		}
		return response;
	}

	private void processXmlFile(String bucketName, String fileName) {
		S3Object s3Object = null;
		BufferedReader reader = null;
		try {
			s3Object = s3Client.getObject(bucketName, fileName);

			reader = new BufferedReader(new InputStreamReader(s3Object.getObjectContent(), "UTF-8"));
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			String ls = System.getProperty("line.separator");
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
			// delete the last new line separator
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
			reader.close();

			String content = stringBuilder.toString();

			content = content.replace("<![CDATA[<itemResponse>", "<itemResponse>");
			content = content.replace("</itemResponse>]]>", "</itemResponse>");

			JSONObject xmlJSONObj = XML.toJSONObject(content);
			xmlJSONObj.put("messageTypeId", "2");
			xmlJSONObj.put("messageType", "itemResponse");
			xmlJSONObj.put("bucketName", bucketName);
			content = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);

			publishToSNS(content);

		} catch (AmazonServiceException ase) {
			LOGGER.error(ase.getMessage(), ase);
			String message = String.format(
					"S3 rejected:Error Message:%s,HTTP Status Code:%s,AWS Error Code:%s,Error Type:%s,Request ID:%s",
					ase.getMessage(), ase.getStatusCode(), ase.getErrorCode(), ase.getErrorType(), ase.getRequestId());

			throw new RuntimeException(message, ase);
		} catch (AmazonClientException ace) {
			LOGGER.error(ace.getMessage(), ace);
			throw new RuntimeException(ace);

		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);

		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (s3Object != null) {
					s3Object.close();
				}
			} catch (Exception ex) {
				LOGGER.error(ex.getMessage(), ex);
				throw new RuntimeException(ex);
			}
		}

	}

	private void publishToSNS(String messageStr) {
		snsClient.publish(snsArn, messageStr);
		//System.out.println(messageStr);
	}

}
