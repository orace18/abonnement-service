
package com.clubchretien.abonnement.xss;

/**
 * User: dev.bscako
 * Date: 08/07/2021 Time: 20:09
 */


public class Validation {
	public static final int ROLES_MIN_LENGTH = 1;
	public static final int PASSWORD_MIN_LENGTH = 12;
	public static final int NAME_MIN_LENGTH = 2;
	public static final int NAME_MAX_LENGTH = 255;
	public static final String PATTERN_PASSWORD = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[$@!%*#?&.])[A-Za-z0-9$@!%*#?&.]{12,}$";
	public static final String PATTERN_NAME = "^([a-zA-Z]+)([a-zA-Z' .-]+)?([a-zA-Z .]+)$";
	public static final String PATTERN_INVOICE_NAME = "^([a-zA-ZéÉèïëê]+)([a-zA-Z' éÉèïëê-]+)?([a-zA-ZéÉèïëê]+)$";
	public static final String PATTERN_COUNTRY = "^([a-zA-ZéÉ]+)([a-zA-ZéÉèïëê' ()-]+)([a-zA-Z)]+)$";
	public static final String PATTERN_CITY = "^([a-zA-ZéÉ]+)([a-zA-Z0-9éÉèïëê' ()-]+)([a-zA-Z0-9)]+)$";
	public static final int COUNTRY_MAX_LENGTH = 34;
	public static final int COUNTRY_MIN_LENGTH = 3;
	public static final String PATTERN_LANGUAGE_CODE = "^[a-z]{2}(-[A-Z]{2})?$";
	public static final int LANGUAGE_CODE_MIN_LENGTH = 2;
	public static final int LANGUAGE_CODE_MAX_LENGTH = 5;
	public static final int LOGIN_MAX_LENGTH = 32;
	public static final int LOGIN_MIN_LENGTH = 6;
	public static final int PHONE_MAX_LENGTH = 13;
	public static final int PHONE_MIN_LENGTH = 8;
	public static final String PATTERN_LOGIN = "^[a-z]([a-z0-9]+)[.]?[a-z0-9]+$";
	public static final String PATTERN_EMAIL = "^[a-z0-9._-]+@[a-z0-9.-]+[.][a-z]{2,4}$";
	public static final String PATTERN_PHONE = "^(([+]|[0]{2})[0-9]{1,3}[ ]?)?((([0-9]{2}[ ]?){3})|([0-9]{3}[ ]?){2})[0-9]{2,3}$";
	public static final String PATTERN_TOKEN = "(^[\\w-]*\\.[\\w-]*\\.[\\w-]*$)";
	public static final String PATTERN_TAX_IDENTIFIER = "^([0-9]{13})$";
	public static final int TAX_IDENTIFIER_LENGTH = 13;
	public static final String PATTERN_DEVICE_IDENTIFIER = "^([A-Z0-9]{10})([-][0-9])?$";
	public static final int DEVICE_IDENTIFIER_MIN_LENGTH = 10;
	public static final String FORMAT_INSTANT = "yyyy-MM-dd HH:mm:ss";
	public static final String TIMEZONE_INSTANT = "UTC";
	public static final String PATTERN_SIGNATURE = "^([A-Z0-9]{4}-){5}[A-Z0-9]{4}$";
	public static final String PATTERN_QRCODE = "^F;[A-Z0-9]{10};[A-Z0-9]{24};[0-9]{13};[0-9]{8,}$";
	public static final String PATTERN_DATE = "dd/MM/yyyy HH:mm:ss";

	public static final int ADDRESS_MAX_LENGTH = 255;
	public static final int ADDRESS_MIN_LENGTH = 2;
	public static final String PATTERN_ADDRESS = "^[a-zA-Z]+([a-zA-Z0-9, /()-]+)?[a-zA-Z0-9)]$";

	public static final int PRODUCT_NAME_MIN_LENGTH = 2;
	public static final int PRODUCT_NAME_MAX_LENGTH = 255;
	public static final String PATTERN_PRODUCT_NAME = "^([a-zA-Z]+)([a-zA-Z' -]+)?([a-zA-Z]+)$";
	public static final String PATTERN_PRODUCT_DETAILS = "^([a-zA-Z]+)([a-zA-Z0-9', ./-]+)?([a-zA-Z0-9%]+)$";

	public static final int HIERARCHY_MIN_LENGTH = 1;
	public static final int HIERARCHY_MAX_LENGTH = 255;
	public static final String PATTERN_HIERARCHY = "^([/][A-Z]+([A-Z0-9]+)?)+$";

	public static final String PATTERN_NUMBER = "^[0-9]+$";

	public static final int PERIOD_MIN_LENGTH = 4;
	public static final int PERIOD_MAX_LENGTH = 4;
	public static final String PATTERN_YEAR = "^[0-9]{4}$";

	public static final int TRUCK_MIN_LENGTH = 2;
	public static final int TRUCK_MAX_LENGTH = 255;
	public static final String PATTERN_TRUCK = "^[A-Z]{2}[ ][0-9]{4}[ ]/[ ][A-Z]{2}[ ][0-9]{4}[ ]RB$";

	public static final int OC_MIN_LENGTH = 2;
	public static final int OC_MAX_LENGTH = 255;
	public static final String PATTERN_OC = "^[A-Z0-9]+([-]?[A-Z0-9]+)$";

	public static final int PLACE_MIN_LENGTH = 2;
	public static final int PLACE_MAX_LENGTH = 255;
	public static final String PATTERN_PLACE = "^[A-Za-z]+([' /-]?[A-Za-z0-9]+)$";

	public static final int LV_MIN_LENGTH = 2;
	public static final int LV_MAX_LENGTH = 255;
	public static final String PATTERN_LV = "^[A-Za-z0-9]+([-]?[A-Za-z0-9]+)$";

	public static final String PATTERN_DATE_REQUEST = "^[0-9]{4}((([-][0-9]{2}){2})|(([/][0-9]{2}){2}))[ T][0-9]{2}([:][0-9]{2}){2}[Z]?$";
	public static final String SEARCH_PATTERN = "^([a-zA-Z ]+)$";
	public static final String SORT_FIELD_PATTERN = "^[a-z]+(([A-Z.])?[a-z]+)+$";
	public static final String SORT_DIRECTION_PATTERN = "^asc|desc|ASC|DESC$";
	public static final String TIME_PATTERN = "^day|DAY|month|MONTH|year|YEAR|day-1|DAY-1|month-1|MONTH-1|year-1|YEAR-1$";

	public static final String CALLBACK_PATTERN = "^(http[s]?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|][/]$";
	public static final String WEBSITE_PATTERN = "^((http[s]?)://)?[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$";

}




