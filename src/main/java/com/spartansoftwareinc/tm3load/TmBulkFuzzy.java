package com.spartansoftwareinc.tm3load;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.xliff.XLIFFFilter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.hibernate.Session;
import com.spartansoftwareinc.tm3load.data.*;
import com.globalsight.ling.tm3.core.TM3Attributes;
import com.globalsight.ling.tm3.core.TM3LeverageMatch;
import com.globalsight.ling.tm3.core.TM3LeverageResults;
import com.globalsight.ling.tm3.core.TM3Locale;
import com.globalsight.ling.tm3.core.TM3MatchType;
import com.globalsight.ling.tm3.core.TM3Tm;
import com.globalsight.ling.tm3.core.TM3Tuv;
import com.globalsight.ling.tm3.tools.TM3Command;

@SuppressWarnings("static-access")
public class TmBulkFuzzy extends TM3Command {
	@Override
	public String getDescription() {
		return "leverage an XLIFF file against the specified tm";
	}

	@Override
	public String getName() {
		return "leverage";
	}

	static final String TM = "tm";
    static final Option TM_OPT = OptionBuilder.withArgName("tm")
                        .hasArg()
                        .withDescription("ID of the TM to leverage against")
                        .isRequired()
                        .create(TM);
                        
    @Override
    public Options getOptions() {
        Options opts = getDefaultOptions();
        opts.addOption(TM_OPT);
        return opts;
    }
	
    @Override
    protected String getUsageLine() {
        return getName() + " [options] -" + TM + " tmId [xliff-file1] [xliff-file2] ...";
    }
    
    @Override
    protected boolean requiresDataFactory() {
    	return true;
    }

	@Override
	protected void handle(Session session, CommandLine command) throws Exception {
		String[] args = command.getArgs();
		if (args.length == 0) usage(); 
		String s = command.getOptionValue(TM);
		
		DataFactory factory = new DataFactory();
        
		// TODO: get these from command line or something
        TM3Locale srcLocale = 
            factory.getLocaleByCode(session, "en_US");
        TM3Locale tgtLocale = 
            factory.getLocaleByCode(session, "fr_FR");

		TM3Tm<Data> tm = getTm(s);
		System.out.println("Got tm " + tm.getId());
		
		for (String arg : args) {
			File input = new File(arg);
			if (!input.exists() || input.isDirectory()) {
				die("Not a file: " + input);
			}
			IFilter filter = new XLIFFFilter();
            filter.open(new RawDocument(input.toURI(), "UTF-8", LocaleId.ENGLISH, LocaleId.FRENCH));
            List<String> segments = new ArrayList<String>();
            while (filter.hasNext()) {
            	Event event = filter.next();
            	if (!event.isTextUnit()) {
            		continue;
            	}
            	ITextUnit tu = event.getTextUnit();
            	segments.add(tu.getSource().toString());
            }
            long totalTime = 0;
            int count = 0, resultCount = 0;
            for (String segment : segments) {
                long start = System.currentTimeMillis();
                TM3LeverageResults<Data> results = 
                    tm.findMatches(new Data(segment, srcLocale), srcLocale, 
                    Collections.singleton(tgtLocale), 
                    TM3Attributes.NONE, TM3MatchType.ALL, false, 5, 0);   
                long end = System.currentTimeMillis();
                count++;
                totalTime += (end - start); // Don't count time spent printing to console
                /*
                System.out.println("Got " + results.getMatches().size() + " results for '" + segment + 
                        "' in " + (end - start) + "ms");
                        */
                for (TM3LeverageMatch<Data> match : results.getMatches()) {
                	/*
                    System.out.println("" + match.getScore() + ": [" + match.getTuv().getContent() + "]");
                    System.out.println("\tTu " + match.getTu().getId() + ", Tuv=" + match.getTuv().getId());
                    */
                    List<TM3Tuv<Data>> targetTuvs = match.getTu().getLocaleTuvs(tgtLocale);
                    /*
                    System.out.println("\tTranslation: [" + targetTuvs.get(0).getContent() + "]");
                    */
                }
                resultCount += results.getMatches().size();
            }
            
            System.out.println("Got " + resultCount + " results for " + count + " queries in " + totalTime + "ms");
		}
	}

}
