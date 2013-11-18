package com.spartansoftwareinc.tm3load;

import com.google.common.collect.Lists;

import javax.xml.namespace.QName;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;

import net.sundell.snax.*;

import javax.xml.stream.*;
import javax.xml.stream.events.*;

// TODO:
// I should really be using some other data structure, this is just a mess.
public class TmxImporter {

	private File tmxFile;
	
	public interface ImportListener {
		void processTu(Tu tu) throws Exception;
	}
	
	public TmxImporter(File tmxFile) {
		this.tmxFile = tmxFile;
	}
	
	public void process(ImportListener listener) 
			throws IOException, XMLStreamException, SNAXUserException {
		Reader r = new BufferedReader(
				new InputStreamReader(new FileInputStream(tmxFile), "UTF-8"));
		// Skip the BOM
		r.mark(1);
		if ((char)r.read() != '\uFEFF') {
			r.reset();
		}
		
		// XXX Simplified version of the TmxReaderThread logic.
		// This assumes that the input data is a lot more sane -- that it's only
		// a single locale pair, things don't need too much localization, etc.
		SNAXParser<ImportListener> parser = 
			SNAXParser.createParser(XMLInputFactory.newInstance(), 
				new NodeModelBuilder<ImportListener>() {{
					elements("tmx", "body", "tu").attach(new TuHandler());
					elements("tmx", "body", "tu", "tuv").attach(new TuvHandler());
				}}.build());
        parser.parse(r, listener);
	}

	private static final QName Q_TUID = new QName("tuid");
	private static final QName Q_DATATYPE = new QName("datatype");
	private static final QName Q_SRCLANG = new QName("srclang");

	// Internal TU state
	private Tu currentTu;
	List<Tuv> currentTuvs = Lists.newArrayList();
		
	class TuHandler extends DefaultElementHandler<ImportListener> {
		@Override
		public void startElement(StartElement element, ImportListener data) 
				throws SNAXUserException {
			currentTu = new Tu();
			// TODO: this is too strict.  This is actually an optional field
			// and can be a string id.
			try {
				currentTu.setId( 
					Long.parseLong(element.getAttributeByName(Q_TUID).getValue()));
			}
			catch (Exception e) {
				throw new SNAXUserException("Could not parse TU id", e);
			}
			// Optional properties
			String dataType = getOptionalAttribute(element, Q_DATATYPE);
			if (dataType != null) {
				currentTu.setFormat(dataType);
			}
			else {
				currentTu.setFormat("text"); // default
			}
			//currentTu.setSourceLocale(getLangAttr(element, Q_SRCLANG));
			currentTu.setSourceTmName(tmxFile.getName());
			
			// GlobalSight-defined properties:
			// XXX Assume segment type of "text"
			currentTu.setType("text");
			// XXX Skip SID
			// XXX Assume translatable ('T')
			
		}
		
		@Override
		public void endElement(EndElement element, ImportListener data) 
					throws SNAXUserException {
            if (currentTuvs.size() < 2) {
                // Empty TU or no targets
                return;
            }
			for (Tuv tuv : currentTuvs) {
				currentTu.addTuv(tuv);
				tuv.setTu(currentTu);
			}
			try {
				data.processTu(currentTu);
			} catch (Exception e) {
				throw new SNAXUserException(e);
			}			
			// Reset data
			currentTuvs.clear();
		}
	}
	
	private static final QName Q_XML_LANG = 
				new QName("http://www.w3.org/XML/1998/namespace", "lang");
	private static final QName Q_CREATIONDATE = new QName("creationdate");
	private static final QName Q_CHANGEDATE = new QName("changedate");

	class TuvHandler extends DefaultElementHandler<ImportListener> {
		private Tuv currentTuv;
		
		class SegHandler extends DefaultElementHandler<ImportListener> {
			@Override
			public void characters(StartElement parent, Characters contents, ImportListener data) {
				// XXX I've removed the part where this is stored as XML,
				// for simplicity
			    currentTuv.appendToSegment(contents.getData());
			}
		}
		
		@Override
		public void build(NodeModelBuilder<ImportListener> builder) {
			builder.element("seg").attach(new SegHandler());
		}
		
		@Override
		public void startElement(StartElement element, ImportListener data) 
							throws SNAXUserException {
			currentTuv = new Tuv();
			// XXX: setOrgSegment is supposed to be XML representation of this!
			// Obviously, I am skipping this completely.
			
			// TODO: I am stubbing out locale
			//currentTuv.setLocale(getLangAttr(element, Q_XML_LANG));
		    // XXX Skipping creationid, which should look at the TUV and then
		    // default to the parent tu's value, if there is one
		    // XXX Ditto with creationuser and modifyuser
		    
		    // XXX This should inherit from parent tu if there's no value
		    String ts = getOptionalAttribute(element, Q_CREATIONDATE);
		    if (ts != null) 		    {
		        Date date = GSUtils.parseUTCNoSeparators(ts);
		        if (date == null) {
		            date = GSUtils.parseUTC(ts);
		        }
		        currentTuv.setCreationDate(new Timestamp(date.getTime()));
		    }
		    else {
		    	currentTuv.setCreationDate(new Timestamp(System.currentTimeMillis()));
		    }
		    // Modification date - only set if known (note: currently
		    // AbstractTmTuv sets the modification date to NOW)
		    // XXX Again, this should inherit from the TU if there is none
		    ts = getOptionalAttribute(element, Q_CHANGEDATE);
		    if (ts != null)
		    {
		        Date date = GSUtils.parseUTCNoSeparators(ts);
		        if (date == null) {
		            date = GSUtils.parseUTC(ts);
		        }
		        currentTuv.setModifyDate(new Timestamp(date.getTime()));
		    }
		    // XXX Skipping SID
		}
		
		@Override
		public void endElement(EndElement element, ImportListener data) {
            if (currentTuv.getSegment() != null) {
                currentTuvs.add(currentTuv);
            }
			currentTuv = null;
		}
	}

	private static String getOptionalAttribute(StartElement element, QName qname) {
		Attribute attr = element.getAttributeByName(qname);
		if (attr != null) {
			return attr.getValue();
		}
		return null;
	}

}
