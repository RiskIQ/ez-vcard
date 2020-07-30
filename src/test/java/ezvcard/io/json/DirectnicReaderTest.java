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
public class DirectnicReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("directnic-example.json"));
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
                .value("Damon DoRemus")
                .noMore();

        asserter.listProperty(Organization.class)
                .values("JD Young")
                .noMore();

        asserter.address()
                .poBox("8221 E. 61st Street. Suite B")
                .locality("Tulsa")
                .region("OK")
                .postalCode("74133")
                .country("US")
                .noMore();

        asserter.telephone()
                .uri(new TelUri.Builder("+1.9183694335").build())
                .types(TelephoneType.VOICE)
                .next()
                .uri(TelUri.parse("tel:"))
                .types(TelephoneType.FAX)
                .noMore();
        asserter.email()
                .value("dns@geekrescue.com")
                .noMore();

        asserter.warnings(29);

        asserter.validate().run();
    }

}
