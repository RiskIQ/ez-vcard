package ezvcard.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.VCard;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
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
public class PandiReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("pandi-example.json"));
        JsonFactory factory = new JsonFactory(new ObjectMapper());
        JsonParser parser = factory.createParser(r);
        parser.nextToken();
        parser.nextToken();
        parser.nextToken();
        JCardReader reader = new JCardReader(parser);
        VCardAsserter asserter = new VCardAsserter(reader);

        asserter.next(V4_0);

        asserter.listProperty(Organization.class)
                .values("Digital Registra")
                .param("TYPE", "work")
                .noMore();
        asserter.email()
                .value("info@digitalregistra.co.id")
                .types(EmailType.WORK)
                .noMore();
        asserter.address()
                .extendedAddress("Jl. lempongsari no. 39C")
                .streetAddress("Jongkang RT/RW 12/35 Sariharjo ")
                .locality("Sleman")
                .region("Yogyakarta")
                .postalCode("55281")
                .country("ID")
                .noMore();
        asserter.telephone()
                .uri(TelUri.parse("tel:0274882257"))
                .types(TelephoneType.VOICE)
                .noMore();

        asserter.warnings(1);

        VCard vcard = asserter.getVCard();
        asserter.validate().prop(vcard.getFormattedName(), 1).run();
    }

}
