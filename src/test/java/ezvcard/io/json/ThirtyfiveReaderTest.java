package ezvcard.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.property.FormattedName;
import ezvcard.property.asserter.VCardAsserter;
import ezvcard.util.Utf8Reader;
import org.junit.Test;

import java.io.Reader;

import static ezvcard.VCardVersion.V4_0;

/**
 * @author dpon
 * created 7/29/20
 */
public class ThirtyfiveReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("thirtyfive-example.json"));
        JsonFactory factory = new JsonFactory(new ObjectMapper());
        JsonParser parser = factory.createParser(r);
        parser.nextToken();
        parser.nextToken();
        parser.nextToken();
        JCardReader reader = new JCardReader(parser);
        VCardAsserter asserter = new VCardAsserter(reader);

        asserter.next(V4_0);

        asserter.simpleProperty(FormattedName.class)
                .value("35.com, Inc.")
                .noMore();

        asserter.validate().run();
    }

}
