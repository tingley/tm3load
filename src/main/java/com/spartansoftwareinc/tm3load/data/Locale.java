package com.spartansoftwareinc.tm3load.data;

import com.globalsight.ling.tm3.core.TM3Locale;

/**
 * Hardcoded set of test locales.
 */
public enum Locale implements TM3Locale {

    EN_US(1, "en", "US"),
    FR_FR(2, "fr", "FR"),
    DE_DE(3, "de", "DE"),
    ES_ES(4, "es", "ES"),
    JP_JP(5, "jp", "JP"),
    ZH_CN(6, "zh", "CN"),
    EN(7, "en"),
    FR(8, "fr");
    
    private long id;
    private String language;
    private String locale;
    private String code;
    
    Locale(long id, String language) {
    	this.id = id;
    	this.language = language;
    	this.code = language;
    }
    
    Locale(long id, String language, String locale) {
        this.id = id;
        this.language = language;
        this.locale = locale;
        this.code = language + "_" + locale;
    }
    
    public long getId() {
        return id;
    }
    
    public String getCode() {
        return code;
    }
    
    public static Locale byCode(String code) {
        for (Locale l : values()) {
            if (l.code.equalsIgnoreCase(code)) {
                return l;
            }
        }
        return null;
    }
    
    public static Locale byId(long id) {
        for (Locale l : values()) {
            if (l.id == id) {
                return l;
            }
        }
        return null;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public String getLocaleCode() {
        return locale;
    } 
}
