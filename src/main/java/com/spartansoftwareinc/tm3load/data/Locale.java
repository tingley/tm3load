package com.spartansoftwareinc.tm3load.data;

import com.globalsight.ling.tm3.core.TM3Locale;

/**
 * Hardcoded set of test locales.
 */
public enum Locale implements TM3Locale {

    EN(1, "en", "US"),
    FR(2, "fr", "FR"),
    DE(3, "de", "DE"),
    ES(4, "es", "ES"),
    JP(5, "jp", "JP"),
    ZH(6, "zh", "CN");
    
    private long id;
    private String language;
    private String locale;
    private String code;
    
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
