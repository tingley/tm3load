package com.spartansoftwareinc.tm3load;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Code borrowed from GlobalSight.
 */
public class GSUtils {
    /**
     * <P>
     * Encodes special XML characters in a string (&lt;, &gt;, &amp;, &apos; and
     * &quot;).
     * </P>
     */
	// from com.globalsight.util.edit.EditUtil
    static public String encodeXmlEntities(String s)
    {
        if (s == null || s.length() == 0)
        {
            return s;
        }

        StringBuffer res = new StringBuffer(s.length());

        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);

            switch (c)
            {
                case '<':
                    res.append("&lt;");
                    break;
                case '>':
                    res.append("&gt;");
                    break;
                case '&':
                    res.append("&amp;");
                    break;
                case '\'':
                    res.append("&apos;");
                    break;
                case '"':
                    res.append("&quot;");
                    break;
                default:
                    res.append(c);
                    break;
            }
        }

        return res.toString();
    }

    static public final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    static public final String FORMAT_COMPRESSED = "yyyyMMdd'T'HHmmss'Z'";

    /**
     * Parses the default (human-readable) UTC String representation
     * into a Java Date.
     */
    public static Date parseUTC(String p_date)
    {
        SimpleDateFormat formatter = new SimpleDateFormat (FORMAT);

        ParsePosition pos = new ParsePosition(0);

        return formatter.parse(p_date, pos);
    }

    /**
     * Parses the compressed (machine-readable) UTC String
     * representation into a Java Date.
    */
    public static Date parseUTCNoSeparators(String p_date)
    {
        SimpleDateFormat formatter = new SimpleDateFormat (FORMAT_COMPRESSED);

        ParsePosition pos = new ParsePosition(0);

        return formatter.parse(p_date, pos);
    }
    
}
