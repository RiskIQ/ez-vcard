package ezvcard.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.FormattedName;
import ezvcard.property.Organization;
import ezvcard.property.asserter.VCardAsserter;
import ezvcard.util.TelUri;
import ezvcard.util.Utf8Reader;
import org.junit.Test;

import java.io.Reader;

import static ezvcard.VCardVersion.V4_0;

/**
 * @author dpon
 * created 7/29/20
 */
public class One0OneDomainReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("101domain-example.json"));
        JsonFactory factory = new JsonFactory(new ObjectMapper());
        JsonParser parser = factory.createParser(r);
        parser.nextToken();
        parser.nextToken();
        parser.nextToken();
        JCardReader reader = new JCardReader(parser);
        VCardAsserter asserter = new VCardAsserter(reader);

        asserter.next(V4_0);

        //@formatter:off
        asserter.simpleProperty(FormattedName.class)
                .value("101domain GRS Ltd.")
                .noMore();

        asserter.listProperty(Organization.class)
                .values("101domain GRS Ltd.")
                .noMore();

        // yes - the address is incorrectly parsed. but that's because they provide the address incorrectly formatted
        asserter.address()
                .streetAddress("4th Floor, International House")
                .locality("3 Harbourmaster Place")
                .region("IFSC")
                .postalCode("Dublin")
                .country("D")
                .noMore();

        asserter.validate().run();
    }

}
