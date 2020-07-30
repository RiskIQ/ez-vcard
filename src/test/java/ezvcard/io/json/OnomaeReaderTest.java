package ezvcard.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.parameter.AddressType;
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
public class OnomaeReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("onomae-example.json"));
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
                .value("Whois Privacy Protection Service by onamae.com")
                .noMore();

        asserter.listProperty(Organization.class)
                .values("Whois Privacy Protection Service by onamae.com")
                .noMore();

        asserter.address()
                .extendedAddress("Cerulean Tower 11F")
                .streetAddress("26-1 Sakuragaoka-cho")
                .locality("Shibuya-ku")
                .region("Tokyo")
                .postalCode("150-8512")
                .country("JP")
                .types(AddressType.WORK)
                .noMore();

        asserter.telephone()
                .text("81.35456256")
                .types(TelephoneType.VOICE)
                .next()
                .text("")
                .types(TelephoneType.FAX)
                .noMore();

        asserter.validate().run();
    }

}
