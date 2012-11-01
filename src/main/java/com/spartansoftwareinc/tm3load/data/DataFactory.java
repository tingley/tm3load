package com.spartansoftwareinc.tm3load.data;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;

import com.globalsight.ling.tm3.core.EditDistanceScorer;
import com.globalsight.ling.tm3.core.TM3DataFactory;
import com.globalsight.ling.tm3.core.TM3FuzzyMatchScorer;
import com.globalsight.ling.tm3.core.TM3Locale;

public class DataFactory implements TM3DataFactory<Data> {

    @Override
    public Configuration extendConfiguration(Configuration cfg) {
        return cfg;
    }

    @Override
    public Data fromSerializedForm(TM3Locale locale, String s) {
        return new Data(s, locale);
    }

    @Override
    public TM3FuzzyMatchScorer<Data> getFuzzyMatchScorer() {
        return new EditDistanceScorer<Data, Data.ScoringToken>();
    }

    @Override
    public TM3Locale getLocaleByCode(Session session, String code) {
        return Locale.byCode(code);
    }

    @Override
    public TM3Locale getLocaleById(Session session, long id) {
        return Locale.byId(id);
    }

}
