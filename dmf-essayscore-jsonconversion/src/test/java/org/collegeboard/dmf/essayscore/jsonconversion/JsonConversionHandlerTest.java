package org.collegeboard.dmf.essayscore.jsonconversion;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
//TODO
@Ignore
public class JsonConversionHandlerTest {

    private static JsonConversionRequest input;

    @BeforeClass
    public static void createInput() throws IOException {
        input = TestUtils.parse("JsonConversionRequest.json", JsonConversionRequest.class);
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }

    @Test
    public void testLambdaFunctionHandler() {
        JsonConversionHandler handler = new JsonConversionHandler();
        Context ctx = createContext();

        Object output = handler.handleRequest(input, ctx);

        // TODO: validate output here if needed.
        if (output != null) {
            System.out.println(output.toString());
        }
    }
}
