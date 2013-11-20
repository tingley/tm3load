package com.spartansoftwareinc.tm3load;

import java.util.*;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import com.spartansoftwareinc.tm3load.data.*;

import com.globalsight.ling.tm3.core.*;
import com.google.common.base.Joiner;
import java.util.Collections;

public class TmFuzzyLookup {

	// This is broken
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: TmFuzzyLookup [tmid] [string] [attr-val]");
                System.exit(1);
            }
            long tmId = 0;
            try {
                tmId = Long.valueOf(args[0]);
            }
            catch (NumberFormatException e) {
                System.err.println("Usage: TmFuzzyLookup [tmid] [string]");
                System.exit(1);
            }
            String attrVal = null;
            //String attrVal = args[args.length - 1];
            
            SessionFactory sessionFactory = null; // TmImport.setupHibernate();
            Session session = sessionFactory.openSession();

            DataFactory factory = new DataFactory();
             
            TM3Locale srcLocale = 
                factory.getLocaleByCode(session, "en-US");
            TM3Locale tgtLocale = 
                factory.getLocaleByCode(session, "fr-FR");
            
            TM3Manager<Data> manager = DefaultManager.create(session);
            TM3Tm<Data> tm = manager.getTm(factory, tmId);
            System.out.println("Got tm " + tm.getId());
            Joiner joiner = Joiner.on(' ');
            //String s = joiner.join(Arrays.asList(args).subList(1, args.length - 1));
            String s = joiner.join(Arrays.asList(args).subList(1, args.length));
            System.out.println("Query: [" + s + "]");
            
            long start = System.currentTimeMillis();
            Map<TM3Attribute, Object> attrs = TM3Attributes.NONE;
            if (attrVal != null) {
                TM3Attribute attr = tm.getAttributeByName("test");
                if (attr != null) {
                    System.out.println("Querying for @test=" + attrVal);
                    attrs = TM3Attributes.one(attr, attrVal);
                }
            }
            TM3LeverageResults<Data> results = 
                tm.findMatches(new Data(s, srcLocale), srcLocale, 
                Collections.singleton(tgtLocale),
                attrs, TM3MatchType.ALL, false, 10, 0);
            long end = System.currentTimeMillis();
            System.out.println("Got " + results.getMatches().size() + " results in " +
                    (end - start) + "ms");
            for (TM3LeverageMatch<Data> match : results.getMatches()) {
                System.out.println("" + match.getScore() + ": [" + match.getTuv().getContent() + "]");
                System.out.println("\tTu " + match.getTu().getId() + ", Tuv=" + match.getTuv().getId());
                List<TM3Tuv<Data>> targetTuvs = match.getTu().getLocaleTuvs(tgtLocale);
                System.out.println("\tTranslation: [" + targetTuvs.get(0).getContent() + "]");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
