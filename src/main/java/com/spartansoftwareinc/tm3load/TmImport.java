package com.spartansoftwareinc.tm3load;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import net.sundell.snax.SNAXUserException;

import javax.xml.stream.Location;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.hibernate.Session;
import com.globalsight.ling.tm3.core.*;
import com.globalsight.ling.tm3.tools.TM3Command;
import com.spartansoftwareinc.otter.TU;
import com.spartansoftwareinc.otter.TUV;
import com.spartansoftwareinc.otter.TUVContent;
import com.spartansoftwareinc.otter.TextContent;
import com.spartansoftwareinc.tm3load.data.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@SuppressWarnings("static-access")
public class TmImport extends TM3Command {

	private int batchSize;
	private TM3Tm<Data> tm;
    private TM3Event event;
	private long startTime;
	private long lastTime;
	
	@Override
	public String getDescription() {
		return "import a TMX file into the specified TM";
	}

	@Override
	public String getName() {
		return "import";
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
	
    // TODO: why did it want src/tgt locale?
    //"Usage: Usage: TmImport [id] [srclocale] [tgtlocale] [file]"
    @Override
    protected String getUsageLine() {
        return getName() + " [options] -" + TM + " tmId [tmx file] [tmx file] ...";
    }
    
    @Override
    protected boolean requiresDataFactory() {
    	return true;
    }

	public void doImport(File tmxFile, int batchSize) throws Exception {
	    if (tm == null) {
	        throw new IllegalStateException("setTm() was never called");
	    }
		this.batchSize = batchSize;
		try {
            this.event = tm.addEvent(0, "TmImport", 
                                     "Import of " + tmxFile.getName());
			startTime = System.currentTimeMillis();
			lastTime = startTime;
			TmxImporter importer = new TmxImporter(tmxFile);
			TmImportListener listener = new TmImportListener();
			importer.process(listener);
			listener.flush();
			System.out.println("Imported " + listener.getCount() + " TUs in " + 
			(System.currentTimeMillis() - startTime) + "ms");
		}
		catch (SNAXUserException e) {
            Location location = e.getLocation();
            System.err.println("ERROR: " + e.getMessage());
            System.err.println("at line " + location.getLineNumber() + 
                " col " + location.getColumnNumber());
			e.printStackTrace();
		}
	}

	class TmImportListener implements TmxImporter.ImportListener {
		private int count = 0;
		private List<TU> tus = Lists.newArrayList();
		private Locale srcLocale;
		
		@Override
		public void setSrcLang(String lang) {
			srcLocale = Locale.byCode(lang);
			if (srcLocale == null) {
				throw new IllegalStateException("Unsupported source locale: " + lang);
			}
		}
		
		@Override
		public void processTu(TU tu) throws SQLException {
			tus.add(tu);
			count++;
			if (tus.size() >= batchSize) {
				flush();
			}
		}
		
		public void flush() throws SQLException {
			if (srcLocale == null) {
				throw new IllegalStateException("No srcLang declared in TMX header!");
			}
		    for (TU tu : tus) {
		        Map<TM3Locale, Data> tgts = Maps.newHashMap();
		        String srcTuvContent = null;
		        for (Map.Entry<String, TUV> entry : tu.getTuvs().entrySet()) {
		        	if (entry.getKey().equalsIgnoreCase(srcLocale.toString())) {
		        		srcTuvContent = flatten(entry.getValue());
		        		continue;
		        	}
		        	Locale targetLocale = Locale.byCode(entry.getKey());
		        	if (targetLocale == null) {
		        		throw new IllegalStateException("Unsupported locale: " + entry.getKey());
		        	}
		        	String tgtSeg = flatten(entry.getValue());
		        	tgts.put(targetLocale, new Data(tgtSeg, targetLocale));
		        }
		        // If there was no src, just skip it
		        if (srcTuvContent == null) {
		        	System.out.println("Skipping TU with no source: " + tu);
		        	continue;
		        }
                long s = System.currentTimeMillis();
                Map<TM3Attribute, Object> attrs = TM3Attributes.NONE;
                TM3Tu<Data> dbtu = tm.save(srcLocale, 
                    new Data(srcTuvContent, srcLocale), 
                    attrs, tgts, TM3SaveMode.MERGE, event);
                long e = System.currentTimeMillis();
                long delta = e - s;
                if (delta > 20) {
                    System.out.println("Slow save (" + delta + "ms) for " +
                            " TU " + tu.getId() + ", DBTU " + dbtu.getId() + " srclen " + 
                            srcTuvContent.length());
                }
		    }
		    commitAndRestartTransaction();
			tus.clear();
			long time = System.currentTimeMillis();
			System.out.println("" + count + " took " + (time - lastTime) + "ms");
			lastTime = time;
		}
		
		public int getCount() {
			return count;
		}
	}

	private String flatten(TUV tuv) {
		StringBuilder sb = new StringBuilder();
		int nextTagId = 1;
		for (TUVContent c : tuv.getContents()) {
			if (c instanceof TextContent) {
				sb.append(((TextContent)c).getValue());
			}
			else {
				sb.append("{").append("" + nextTagId++).append("}");
			}
		}
		return sb.toString();
	}
	
	@Override
	protected void handle(Session session, CommandLine command) throws Exception {
		String[] args = command.getArgs();
		if (args.length == 0) usage(); 
		String s = command.getOptionValue(TM);
		
		this.tm = getTm(s);

		for (String arg : args) {
			File tmxFile = new File(arg);
    		if (!tmxFile.exists() || tmxFile.isDirectory()) {
    			die("Not a file: " + tmxFile);
    		}
    		System.out.println("Importing from " + tmxFile);
			doImport(tmxFile, 100);
		}
	}
}
