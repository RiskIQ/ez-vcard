package ezvcard.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.FormattedName;
import ezvcard.property.asserter.VCardAsserter;
import ezvcard.util.TelUri;
import ezvcard.util.Utf8Reader;
import org.junit.Test;

import java.io.Reader;

import static ezvcard.VCardVersion.V4_0;

/**
 * @author bdrake
 * created 6/9/21
 */
public class DreamscapeReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("dreamscape-example.json"));
        JsonFactory factory = new JsonFactory(new ObjectMapper());
        JsonParser parser = factory.createParser(r);
        parser.nextToken();
        parser.nextToken();
        parser.nextToken();
        JCardReader reader = new JCardReader(parser);
        VCardAsserter asserter = new VCardAsserter(reader);

        asserter.next(V4_0);

        asserter.simpleProperty(FormattedName.class)
                .value("Robert James Brook")
                .noMore();

        asserter.email()
                .value("robbrook@bigpond.com")
                .types(EmailType.WORK)
                .noMore();

        TelUri teluri = new TelUri.Builder("+61-429465137").build();

        asserter.telephone()
                .uri(teluri)
                .types(TelephoneType.VOICE)
                .noMore();

        asserter.address()
                .types(AddressType.WORK)
                .streetAddress("32 Cardiff Rd")
                .locality("New Lambton Hts")
                .region("NSW")
                .postalCode("2305")
                .country("AU")
                .noMore();

        asserter.validate().run();
    }

}
