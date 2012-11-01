package com.spartansoftwareinc.tm3load;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sundell.snax.SNAXUserException;
import javax.xml.stream.Location;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.globalsight.ling.tm3.core.*;
import com.globalsight.ling.tm3.core.persistence.HibernateConfig;

import com.spartansoftwareinc.tm3load.data.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TmImport {

	private int batchSize;
	private TM3Tm<Data> tm;
    private TM3Event event;
	private long startTime;
	private long lastTime;
	private Session session;
	private Transaction tx = null;
	private static TM3Locale srcLocale, tgtLocale;
	
	public TmImport(Session session, TM3Tm<Data> tm) {
	    this.session = session;
	    this.tm = tm;
	}

	public void doImport(File tmxFile, int batchSize) throws Exception {
	    if (tm == null) {
	        throw new IllegalStateException("setTm() was never called");
	    }
	    // TODO: load src, tgt locales
		this.batchSize = batchSize;
		tx = session.beginTransaction();
		try {
            this.event = tm.addEvent(0, "tingley", 
                                     "Import of " + tmxFile.getName());
			startTime = System.currentTimeMillis();
			lastTime = startTime;
			TmxImporter importer = new TmxImporter(tmxFile);
			TmImportListener listener = new TmImportListener();
			importer.process(listener);
			listener.flush();
			tx.commit();
			System.out.println("Imported " + listener.getCount() + " TUs in " + 
			(System.currentTimeMillis() - startTime) + "ms");
		}
		catch (SNAXUserException e) {
            Location location = e.getLocation();
            System.err.println("ERROR: " + e.getMessage());
            System.err.println("at line " + location.getLineNumber() + 
                " col " + location.getColumnNumber());
			e.printStackTrace();
			if (tx != null) {
				tx.rollback();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			if (tx != null) {
				tx.rollback();
			}
		}
	}

    static int nextAttrValue = 0;
    static TM3Attribute attr = null;

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
                String prefix = (contentPrefix != null) ? 
                        contentPrefix + " " : "";
                Map<TM3Attribute, Object> attrs = (attr == null) ?
                    TM3Attributes.NONE :
                    TM3Attributes.one(attr, "" + nextAttrValue++);
                nextAttrValue %= 4;
                TM3Tu<Data> dbtu = tm.save(srcLocale, 
                    new Data(prefix +  tu.getSourceTuv().getSegment(), srcLocale), 
                    attrs, tgts, TM3SaveMode.MERGE, event);
                long e = System.currentTimeMillis();
                long delta = e - s;
                if (delta > 20) {
                    System.out.println("Slow save (" + delta + "ms) for " +
                            " TU " + tu.getId() + ", DBTU " + dbtu.getId() + " srclen " + 
                            tu.getSourceTuv().getSegment().length());
                }
		    }
	        tx.commit();
	        tx = session.beginTransaction();
			tus.clear();
			long time = System.currentTimeMillis();
			System.out.println("" + count + " took " + (time - lastTime) + "ms");
			lastTime = time;
		}
		
		public int getCount() {
			return count;
		}
	}

    private static void usage() {
        System.err.println(
            "Usage: Usage: TmImport [id] [srclocale] [tgtlocale] [file]");
        System.exit(1);
    }

    private static String contentPrefix = "";

	public static void main(String[] args) {
        if (args.length < 4 || args.length > 5) {
            usage();
        }
        long tmId = -1;
        try {
            tmId = Long.valueOf(args[0]);
        }
        catch (Exception e) {}
        if (tmId == -1) {
            usage();
        }
	    try {
	        SessionFactory sessionFactory = setupHibernate();
	        Session session = sessionFactory.openSession();
            DataFactory factory = new DataFactory();
            srcLocale = factory.getLocaleByCode(session, args[1]);
            tgtLocale = factory.getLocaleByCode(session, args[2]);
            if (srcLocale == null || tgtLocale == null) {
                usage();
            }
	        
	        TM3Manager manager = DefaultManager.create();
	        TM3Tm<Data> tm = manager.getTm(session, factory, tmId);
            if (tm == null) {
                System.out.println("No such TM " + tmId);
                usage();
            }
            attr = tm.getAttributeByName("test");
            if (attr != null) {
                System.out.println("Using attribute 'test'");
            }
            TmImport importer = new TmImport(session, tm);
    		File tmxFile = new File(args[3]);
    		if (!tmxFile.exists() || tmxFile.isDirectory()) {
    			System.err.println("Not a file: " + tmxFile);
    			System.exit(1);
    		}
    		System.out.println("Importing from " + tmxFile);
            if (args.length == 5) {
                contentPrefix = args[4];
                System.out.println("Using TUV content prefix '" + 
                                   contentPrefix + "'");
            }
			importer.doImport(tmxFile, 100);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static SessionFactory setupHibernate() {
	    long start = System.currentTimeMillis();
	    Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLInnoDBDialect");
        props.put("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        props.put("hibernate.connection.url", 
                  "jdbc:mysql://localhost:3306/tm3?useUnicode=true&characterEncoding=UTF-8");
        props.put("hibernate.connection.username", "tausdata");
        props.put("hibernate.connection.password", "tau5Data!12");
        props.put("hibernate.cglib.use_reflection_optimizer", "false"); // this is default in hibernate 3.2
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "true");
        // For debug
        props.put("hibernate.connection.pool_size", "1");
        Configuration cfg = new Configuration().addProperties(props);
        cfg = HibernateConfig.extendConfiguration(cfg);
        cfg = new DataFactory().extendConfiguration(cfg);
        SessionFactory sessionFactory = cfg.buildSessionFactory();
        System.out.println("Hibernate initialization took " + 
                           (System.currentTimeMillis() - start) + "ms");
        return sessionFactory;
	}

}
