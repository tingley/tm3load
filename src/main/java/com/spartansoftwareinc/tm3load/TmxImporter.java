package com.spartansoftwareinc.tm3load;

import com.spartansoftwareinc.otter.OtterException;
import com.spartansoftwareinc.otter.TMXEvent;
import com.spartansoftwareinc.otter.TMXEventType;
import com.spartansoftwareinc.otter.TMXReader;
import com.spartansoftwareinc.otter.TU;

import java.io.*;
import javax.xml.stream.*;

public class TmxImporter {

	private File tmxFile;
	
	public interface ImportListener {
		void setSrcLang(String lang);
		void processTu(TU tu) throws Exception;
	}
	
	public TmxImporter(File tmxFile) {
		this.tmxFile = tmxFile;
	}
	
	public void process(ImportListener listener) 
			throws IOException, XMLStreamException, OtterException {
		Reader r = new BufferedReader(
				new InputStreamReader(new FileInputStream(tmxFile), "UTF-8"));
		TMXReader tmxReader = TMXReader.createTMXEventReader(r);
		try {
			while (tmxReader.hasNext()) {
				TMXEvent event = tmxReader.nextEvent();
				if (event.getEventType() == TMXEventType.HEADER) {
					listener.setSrcLang(event.getHeader().getSrcLang());
				}
				else if (event.getEventType() == TMXEventType.TU) {
					listener.processTu(event.getTU());
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
