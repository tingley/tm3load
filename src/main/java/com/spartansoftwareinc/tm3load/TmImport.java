package com.spartansoftwareinc.tm3load;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sundell.snax.SNAXUserException;

import javax.xml.stream.Location;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.globalsight.ling.tm3.core.*;
import com.globalsight.ling.tm3.core.persistence.HibernateConfig;
import com.globalsight.ling.tm3.tools.TM3Command;
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
	private Session session;
	private static TM3Locale srcLocale, tgtLocale;
	
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

	// TODO: use otter
    // Hmm: The wstx parser used by okapi is picked up by snax as well.
    // It is trying to load the DTD!  Does the built-in eventreader do this?
    /*
     * com.ctc.wstx.exc.WstxParsingException: (was java.io.FileNotFoundException) 
     * /Users/chase/Documents/src/tm3load/tmx14.dtd (No such file or directory)
     * org.codehaus.stax2.ri.Stax2EventReaderImpl
     */
	public void doImport(File tmxFile, int batchSize) throws Exception {
	    if (tm == null) {
	        throw new IllegalStateException("setTm() was never called");
	    }
	    // TODO: load src, tgt locales
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
		private List<Tu> tus = Lists.newArrayList();
		
		@Override
		public void processTu(Tu tu) throws SQLException {
			tus.add(tu);
			count++;
			if (tus.size() >= batchSize) {
				flush();
			}
		}
		
		public void flush() throws SQLException {
		    for (Tu tu : tus) {
		        Map<TM3Locale, Data> tgts = Maps.newHashMap();
                for (Tuv tuv : tu.getTargetTuvs()) {
                    tgts.put(tgtLocale, new Data(tuv.getSegment(), tgtLocale));
                }
                long s = System.currentTimeMillis();
                Map<TM3Attribute, Object> attrs = TM3Attributes.NONE;
                TM3Tu<Data> dbtu = tm.save(srcLocale, 
                    new Data(tu.getSourceTuv().getSegment(), srcLocale), 
                    attrs, tgts, TM3SaveMode.MERGE, event);
                long e = System.currentTimeMillis();
                long delta = e - s;
                if (delta > 20) {
                    System.out.println("Slow save (" + delta + "ms) for " +
                            " TU " + tu.getId() + ", DBTU " + dbtu.getId() + " srclen " + 
                            tu.getSourceTuv().getSegment().length());
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

	@Override
	protected void handle(Session session, CommandLine command) throws Exception {
		String[] args = command.getArgs();
		if (args.length == 0) usage(); 
		String s = command.getOptionValue(TM);
		
		DataFactory factory = new DataFactory();
        
		// TODO: get these from command line or something
        srcLocale = factory.getLocaleByCode(session, "en_US");
        tgtLocale = factory.getLocaleByCode(session, "fr_FR");

        this.session = session;
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
