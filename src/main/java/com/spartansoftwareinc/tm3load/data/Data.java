package com.spartansoftwareinc.tm3load.data;

import java.util.List;

import com.globalsight.ling.tm3.core.Fingerprint;
import com.globalsight.ling.tm3.core.TM3Data;
import com.globalsight.ling.tm3.core.TM3FuzzyComparable;
import com.globalsight.ling.tm3.core.TM3Locale;
import com.globalsight.ling.tm3.core.TM3Scorable;
import com.google.common.collect.Lists;

/**
 * Naive data class with naive tokenize() method.
 *
 */
public class Data implements TM3Data, TM3Scorable<Data.ScoringToken> {
    private String text;
    private TM3Locale locale;
    public Data(String text, TM3Locale locale) {
        this.text = text;
        this.locale = locale;
    }

    @Override
    public String getSerializedForm() {
        return text;
    }

    @Override
    public long getFingerprint() {
        return Fingerprint.fromString(text);
    }

    @Override
    public Iterable<Long> tokenize() {
        String[] toks = text.split("\\s+");
        List<Long> values = Lists.newArrayList();
        for (String s : toks) {
            values.add(Fingerprint.fromString(s));
        }
        return values;
    }

    @Override
    public boolean equals(Object o) {
        return o != null &&
                getClass().equals(o.getClass()) &&
                ((Data)o).text.equals(text) &&
                ((Data)o).locale.equals(locale);
    }

    @Override
    public int hashCode() {
        return text.hashCode() + 31*locale.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public List<ScoringToken> getScoringObjects() {
        String[] toks = text.split("\\s+");
        List<ScoringToken> values = Lists.newArrayList();
        for (String s : toks) {
            values.add(new ScoringToken(s));
        }
        return values;
    }
    
    // Hacky token that returns either 1 or 0, no real word-level
    // fuzzyness
    static class ScoringToken implements TM3FuzzyComparable<ScoringToken> {
        private String s;
        ScoringToken(String s) {
            this.s = s;
        }
        
        @Override
        public float fuzzyCompare(ScoringToken tok) {
            return (s.equals(tok.s)) ? 1.0f : 0f;
        }
        
    }
}
