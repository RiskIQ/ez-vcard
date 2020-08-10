package ezvcard.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.FormattedName;
import ezvcard.property.Kind;
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
public class NamecheapDomainReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("namecheap-example.json"));
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
                .value("REACTIVATION PERIOD")
                .noMore();

        asserter.simpleProperty(Kind.class)
                .value("individual")
                .noMore();

        asserter.listProperty(Organization.class)
                .values("NAMECHEAP")
                .noMore();

        asserter.address()
                .streetAddress("11400 W. OLYMPIC BLVD, SUITE 200")
                .locality("LOS ANGELES")
                .region("CA")
                .postalCode("90064")
                .country("US")
                .noMore();

        asserter.telephone()
                .uri(new TelUri.Builder("+1.6613102107").build())
                .types(TelephoneType.VOICE)
                .next()
                .text("fax:+1.6613102107")
                .types(TelephoneType.FAX)
                .noMore();
        asserter.email()
                .value("reactivation-pending@namecheap.com")
                .noMore();

//        asserter.warnings(29);

        asserter.validate().run();
    }

}
