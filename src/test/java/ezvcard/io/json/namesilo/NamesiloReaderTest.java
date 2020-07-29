package ezvcard.io.json.namesilo;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.io.json.JCardReader;
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
public class NamesiloReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("namesilo-example.json"));
        JsonFactory factory = new JsonFactory(new ObjectMapper());
        JsonParser parser = factory.createParser(r);
        parser.nextToken();
        JCardReader reader = new JCardReader(parser);
//        VCard vcard = reader.readNext();
//        System.out.println(vcard);
        VCardAsserter asserter = new VCardAsserter(reader);

        asserter.next(V4_0);

        //@formatter:off
        asserter.simpleProperty(FormattedName.class)
                .value("Domain Administrator")
                .noMore();

        asserter.listProperty(Organization.class)
                .values("See PrivacyGuardian.org")
                .noMore();

        asserter.address()
                .streetAddress("1928 E. Highland Ave. Ste F104", "PMB# 255")
                .locality("Phoenix")
                .region("AZ")
                .postalCode("85016")
                .country("US")
                .noMore();

        asserter.telephone()
                .uri(new TelUri.Builder("+0.3478717726").build())
                .noMore();

        asserter.email()
                .value("pw-a3b2c5eac8da5223be1aea812b2b1e3b@privacyguardian.org")
                .noMore();

        asserter.warnings(29);

        asserter.validate().run();
        asserter.done();
    }

}
