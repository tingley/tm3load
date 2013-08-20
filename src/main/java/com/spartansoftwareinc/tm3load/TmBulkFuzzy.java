package com.spartansoftwareinc.tm3load;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import com.spartansoftwareinc.tm3load.data.*;

import com.globalsight.ling.tm3.core.DefaultManager;
import com.globalsight.ling.tm3.core.TM3Attributes;
import com.globalsight.ling.tm3.core.TM3LeverageMatch;
import com.globalsight.ling.tm3.core.TM3LeverageResults;
import com.globalsight.ling.tm3.core.TM3Locale;
import com.globalsight.ling.tm3.core.TM3Manager;
import com.globalsight.ling.tm3.core.TM3MatchType;
import com.globalsight.ling.tm3.core.TM3Tm;
import com.globalsight.ling.tm3.core.TM3Tuv;

/**
 * Read lines from stdin of things to search for.
 */
public class TmBulkFuzzy {
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println("Usage: TmFuzzyLookup [tmid]");
                System.exit(1);
            }
            long tmId = 0;
            try {
                tmId = Long.valueOf(args[0]);
            }
            catch (NumberFormatException e) {
                System.err.println("Usage: TmBulkFuzzy [tmid] < [strings]");
                System.exit(1);
            }
            
            SessionFactory sessionFactory = TmImport.setupHibernate();
            Session session = sessionFactory.openSession();

            DataFactory factory = new DataFactory();
             
            TM3Locale srcLocale = 
                factory.getLocaleByCode(session, "en-US");
            TM3Locale tgtLocale = 
                factory.getLocaleByCode(session, "es-ES");
            
            TM3Manager<Data> manager = DefaultManager.create(session);
            TM3Tm<Data> tm = manager.getTm(factory, tmId);
            System.out.println("Got tm " + tm.getId());
            
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            long totalTime = 0;
            int count = 0, resultCount = 0;
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                long start = System.currentTimeMillis();
                TM3LeverageResults<Data> results = 
                    tm.findMatches(new Data(line, srcLocale), srcLocale, 
                    Collections.singleton(tgtLocale), 
                    TM3Attributes.NONE, TM3MatchType.ALL, false, 5, 0);   
                long end = System.currentTimeMillis();
                count++;
                totalTime += (end - start); // Don't count time spent printing to console
                System.out.println("Got " + results.getMatches().size() + " results for '" + line + 
                        "' in " + (end - start) + "ms");
                for (TM3LeverageMatch<Data> match : results.getMatches()) {
                    System.out.println("" + match.getScore() + ": [" + match.getTuv().getContent() + "]");
                    System.out.println("\tTu " + match.getTu().getId() + ", Tuv=" + match.getTuv().getId());
                    List<TM3Tuv<Data>> targetTuvs = match.getTu().getLocaleTuvs(tgtLocale);
                    System.out.println("\tTranslation: [" + targetTuvs.get(0).getContent() + "]");
                }
                resultCount += results.getMatches().size();
            }
            
            System.out.println("Got " + resultCount + " results for " + count + " queries in " + totalTime + "ms");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
