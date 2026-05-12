package com.clubchretien.abonnement.constants;

import java.text.SimpleDateFormat;

public interface MobileConstant<Enum> {
    public static Long RESET_PIN_EXPIRATION = 7200000l;
    public static Long SMS_EXPIRATION = 1200000l;
    public static String RESET_PIN_OBJECT = "RESET PIN";
    public static String SMS_CODE = "SMS CODE";
    public static String RESET_PIN_CONSOMMATEUR= "OWOMI MOBILE";
    public static String RESET_PIN_TITLE= "Demande de réinitialisation de PIN";
    public static final String SENS_DEBIT = "0";
    public static final String XOF = "XOF";
    public static final String SENS_CREDIT = "1";
    public static final Long SESSION_DURATION=19000L;
    public static final String OWOMI_ACTIVITE = "activite";
    public static final SimpleDateFormat ACTIVITY_LOG_DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy | HH:mm:ss");
    public static final String OWOMI_CODE_SUITE_2="\nCZJRfljwhTN";
    public static final String MENSUEL = "mensuel";

}

