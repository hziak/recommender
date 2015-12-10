package eu.eexcess.europeana.recommender;

/**
 * Created by hziak on 23.11.15.
 */

import eu.eexcess.dataformats.userprofile.ContextKeyword;
import eu.eexcess.dataformats.userprofile.SecureUserProfile;
import eu.eexcess.dataformats.userprofile.SpecialFieldsEum;
import eu.eexcess.dataformats.userprofile.TimeRange;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.junit.Assert.assertTrue;

public class EuropeanaSpecialFieldsTest {

    public EuropeanaSpecialFields europeanaSpecialFields;

    @Before public void prepareConfig() {
        europeanaSpecialFields = new EuropeanaSpecialFields();

    }

    @Test public void toQueryTestWHOWHAT() {
        SecureUserProfile secureUserProfile = new SecureUserProfile();
        ContextKeyword ck1 = new ContextKeyword();
        ck1.setType(SpecialFieldsEum.WHO);
        ck1.setText("SEPP FORCHER");
        secureUserProfile.getContextKeywords().add(ck1);
        ContextKeyword ck2 = new ContextKeyword();
        ck2.setType(SpecialFieldsEum.WHAT);
        ck2.setText("KLINGENDES ÖSTERREICH");
        secureUserProfile.getContextKeywords().add(ck2);
        String returnString = europeanaSpecialFields.toQuery(secureUserProfile);
        System.out.println(returnString);

        assertTrue(returnString.contains("TYPE:KLINGENDES+%C3%96STERREI"));

    }

    @Test public void toQueryTestFROMTO() {
        SecureUserProfile secureUserProfile = new SecureUserProfile();
        secureUserProfile.setTimeRange(new TimeRange());
        secureUserProfile.getTimeRange().setStart("1970");
        secureUserProfile.getTimeRange().setEnd("2000");

        String returnString = europeanaSpecialFields.toQuery(secureUserProfile);
        String decoded = "";
        try {
            decoded = URLDecoder.decode(returnString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.println(decoded);
        assertTrue(decoded.equals("YEAR:[1970 TO 2000]"));

    }
}