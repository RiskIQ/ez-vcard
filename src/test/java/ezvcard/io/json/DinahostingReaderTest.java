package ezvcard.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.FormattedName;
import ezvcard.property.Organization;
import ezvcard.property.VCardProperty;
import ezvcard.property.asserter.VCardAsserter;
import ezvcard.util.Utf8Reader;
import org.junit.Test;

import java.io.Reader;

import static ezvcard.VCardVersion.V4_0;

/**
 * @author dpon
 * created 7/29/20
 */
public class DinahostingReaderTest {

    @Test
    public void jcard_example() throws Throwable {

        Reader r = new Utf8Reader(getClass().getResourceAsStream("dinahosting-example.json"));
        JsonFactory factory = new JsonFactory(new ObjectMapper());
        JsonParser parser = factory.createParser(r);
        parser.nextToken();
        parser.nextToken();
        parser.nextToken();
        JCardReader reader = new JCardReader(parser);
        VCardAsserter asserter = new VCardAsserter(reader);

        asserter.next(V4_0);

        //@formatter:off

        asserter.listProperty(Organization.class)
                .values("NULL")
                .noMore();

        asserter.address()
                .region("Barcelona")
                .param("CC", "ES")
                .noMore();

//        asserter.(29);
//        asserter.

        asserter.validate().prop(null, 1).run();
    }

}
