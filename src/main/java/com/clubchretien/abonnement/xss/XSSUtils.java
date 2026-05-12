
package com.clubchretien.abonnement.xss;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.owasp.esapi.ESAPI;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * User: BSCAKO
 * Date: 03/06/2021 Time: 12:55
 */


public class XSSUtils {

    public static String PrettyPrintJson(String value) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(value);
        return gson.toJson(je);
    }

    public static Object strip(Object object, boolean withoutExposeAnnotation) {
        if (object == null) {
            return null;
        }
        ;
        if (object instanceof String) {
            return stripXSS((String) object);
        }
        Gson gson = new Gson();
        if (withoutExposeAnnotation) {
            gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        }
        String json = gson.toJson(object);
        json = XSSUtils.stripXSS(json);
        return Utils.SafeJSonReader(gson, json, object.getClass(), object);
    }

    public static String stripXSS(String value) {
        if (value == null) {
            return null;
        }
        value = ESAPI.encoder()
                .canonicalize(value, true)
                .replaceAll("\0", "");
        value = XSSUtils.stripXSSAndSql(value);
        value = Jsoup.clean(value, Whitelist.none());
        return value;
    }

    /**
     * Apply the XSS filter to the parameters in a Map
     *
     * @param parameters
     */
    public static void cleanParams(Map<String, String> parameters) {
        for (Map.Entry<String, String> params : parameters.entrySet()) {
            String key = params.getKey();
            String value = params.getValue();
            parameters.put(key, stripXSS(value));
        }
    }

    /**
     * Prevent xss cross-scripting attacks (replace, adjust according to actual situation)
     */
    public static String stripXSSAndSql(String value) {
        if (value != null) {
            // NOTE: It's highly recommended to use the ESAPI library and
            // uncomment the following line to
            // avoid encoded attacks.
            // value = ESAPI.encoder().canonicalize(value);
            // Avoid null characters
            //  value = value.replaceAll("", "");
            // Avoid anything between script tags
            Pattern scriptPattern = Pattern.compile(
                    "<[\r\n| ]*script[\r\n| ]*>(.*?)</[\r\n| ]*script[\r\n| ]*>", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            // Avoid anything in a
            // src="http://www.yihaomen.com/article/java/..." type of
            // e-xpression
            scriptPattern = Pattern.compile("src[\r\n| | ]*=[\r\n| ]*[\\\"|\\\'](.*?)[\\\"|\\\']",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            // Remove any lonesome </script> tag
            scriptPattern = Pattern.compile("</[\r\n| | ]*script[\r\n| | ]*>", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            // Remove any lonesome <script ...> tag
            scriptPattern = Pattern.compile("<[\r\n| | ]*script(.*?)>",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            // Avoid eval(...) expressions
            scriptPattern = Pattern.compile("eval\\((.*?)\\)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            // Avoid e-xpression(...) expressions
            scriptPattern = Pattern.compile("e-xpression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            // expression(...)
            scriptPattern = Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            // Avoid javascript:... expressions
            scriptPattern = Pattern.compile("javascript[\r\n| | ]*:[\r\n| | ]*", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            // Avoid vbscript:... expressions
            scriptPattern = Pattern.compile("vbscript[\r\n| | ]*:[\r\n| | ]*", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            // Avoid onload= expressions
            scriptPattern = Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            //cmd for xls injection...
            scriptPattern = Pattern.compile("=cmd(.*?)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
        }
        return value;
    }

    public static boolean checkXSSAndSql(String value) {
        boolean flag = false;
        if (value != null) {
            // NOTE: It's highly recommended to use the ESAPI library and
            // uncomment the following line to
            // avoid encoded attacks.
            // value = ESAPI.encoder().canonicalize(value);
            // Avoid null characters
            // value = value.replaceAll("", "");
            // Avoid anything between script tags
            Pattern scriptPattern = Pattern.compile(
                    "<[\r\n| | ]*script[\r\n| | ]*>(.*?)</[\r\n| | ]*script[\r\n| | ]*>", Pattern.CASE_INSENSITIVE);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
            // Avoid anything in a
            // src="http://www.yihaomen.com/article/java/..." type of
            // e-xpression
            scriptPattern = Pattern.compile("src[\r\n| | ]*=[\r\n| | ]*[\\\"|\\\'](.*?)[\\\"|\\\']",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
            // Remove any lonesome </script> tag
            scriptPattern = Pattern.compile("</[\r\n| | ]*script[\r\n| | ]*>", Pattern.CASE_INSENSITIVE);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
            // Remove any lonesome <script ...> tag
            scriptPattern = Pattern.compile("<[\r\n| | ]*script(.*?)>",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
            // Avoid eval(...) expressions
            scriptPattern = Pattern.compile("eval\\((.*?)\\)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
            // Avoid e-xpression(...) expressions
            scriptPattern = Pattern.compile("e-xpression\\((.*?)\\)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
            // Avoid javascript:... expressions
            scriptPattern = Pattern.compile("javascript[\r\n| | ]*:[\r\n| | ]*", Pattern.CASE_INSENSITIVE);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
            // Avoid vbscript:... expressions
            scriptPattern = Pattern.compile("vbscript[\r\n| | ]*:[\r\n| | ]*", Pattern.CASE_INSENSITIVE);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
            // Avoid onload= expressions
            scriptPattern = Pattern.compile("onload(.*?)=",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            flag = scriptPattern.matcher(value).find();
            if (flag) {
                return true;
            }
        }
        return false;
    }


    /**
     * Replace half-width characters that are prone to xss & sql vulnerabilities directly into full-width characters
     *
     * @param s
     * @return
     */
    private static String xssEncode(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        } else {
            s = stripXSSAndSql(s);
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '>':
                    sb.append(">");// escape is greater than
                    break;
                case '<':
                    sb.append("<");// escape less than
                    break;
                // case '\'':
                // sb.append("'");// escape single quotes
                // break;
                // case '\"':
                // sb.append(""");// escape double quotes
                // break;
                case '&':
                    sb.append("&");// Escape &
                    break;
                case '#':
                    sb.append("#");//  #
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }


}




