package com.spartansoftwareinc.tm3load;

import java.util.Map;

import com.globalsight.ling.tm3.tools.TM3Command;
import com.globalsight.ling.tm3.tools.TM3Tool;

public class Tool extends TM3Tool {

	public static void main(String[] args) {
        new Tool().run(args);
    }
	
	@Override
	protected void registerCustomCommands(
            Map<String, Class<? extends TM3Command>> commands) {
		commands.put("import", TmImport.class);
		commands.put("leverage", TmBulkFuzzy.class);
	}
}
