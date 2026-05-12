
package com.clubchretien.abonnement.xss;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Path;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * User: dev.bscako
 * Date: 23/06/2021 Time: 18:28
 */

@Slf4j
public class Utils {

    public static final String BEARER = "Bearer";
    public static final String SPACE = " ";
    public static final String COMMA = ",";
    public static final String EMPTY = "";
    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    public static double CorrectQuantity(double value, int places) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.setScale(places, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    public static boolean IsValue(Boolean data) {
        return data != null && data;
    }

    public static ObjectMapper Mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setDateFormat(new SimpleDateFormat(Validation.FORMAT_INSTANT));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }


    public static <T> Collection<T> Pageable(List<T> data, Pageable pageable) {
        if (pageable == null) {
            return data;
        }
        final int start = (int) pageable.getOffset();
        final int end = Math.min((start + pageable.getPageSize()), data.size());
        final Page<T> page = new PageImpl<>(data.subList(start, end), pageable, data.size());
        data = page.stream().collect(Collectors.toList());
        return data;
    }

    public static String[] MergeArrays(String[]... data) {
        return Stream.of(data).flatMap(Stream::of).toArray(String[]::new);
    }


    public static void Merge(Object obj, Object update) {
        if (!obj.getClass().isAssignableFrom(update.getClass())) {
            return;
        }
        Method[] methods = obj.getClass().getMethods();
        for (Method fromMethod : methods) {
            if (fromMethod.getDeclaringClass().equals(obj.getClass())
                    && fromMethod.getName().startsWith("get")) {

                String fromName = fromMethod.getName();
                String toName = fromName.replace("get", "set");

                try {
                    Method toMetod = obj.getClass().getMethod(toName, fromMethod.getReturnType());
                    Object value = fromMethod.invoke(update, (Object[]) null);
                    if (value != null) {
                        toMetod.invoke(obj, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String StripAccent(String value) {
        if (value == null) {
            return null;
        }
        value = Normalizer.normalize(value, Normalizer.Form.NFD);
        value = value.replaceAll("\\p{M}", "");
        return value;
    }

    public static String Trim(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    public static String TrimPhone(String value) {
        if (value != null && !value.trim().isEmpty()) {
            String phone = value.replaceAll("\\s", "");
            if (phone.trim().isEmpty()) {
                phone = null;
            }
            return phone;
        }
        return null;
    }

    public static String FormatPhone(String value) {
        value = TrimPhone(value);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        if (value.startsWith("+")) {
            return "(" + value.replaceFirst(
                    "(\\d{3})(\\d{2})(\\d{3})(\\d+)",
                    "$1) $2 $3 $4"
            );
        }
        return value.replaceFirst(
                "(\\d{2})(\\d{3})(\\d+)",
                "$1 $2 $3"
        );
    }

    public static int CountMatch(String value, String regex) {
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static Object SafeJSonReader(Gson gson, String json, Class<?> clazz) {
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String ToJson(Gson gson, Object o) {
        return gson.toJson(o);
    }

    public static String ToJson(Object o) {
        return ToJson(o, false);
    }

    public static String ToJson(Object o, boolean beautify) {
        Gson gson = new Gson();
        if (beautify) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }
        return ToJson(gson, o);
    }

    public static boolean IsBearerToken(String token) {
        String bearer = BEARER + SPACE;
        return token != null && token.contains(bearer);
    }

    public static boolean IsToken(String headerKey, String token) {
        return token != null && token.contains(headerKey);
    }

    public static String TrimBearerToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String bearer = BEARER + SPACE;
        return TrimToken(bearer, token);
    }

    public static String TrimToken(String headerKey, String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        if (headerKey == null || headerKey.trim().isEmpty()) {
            return token;
        }
        return token.replace(headerKey, EMPTY);
    }

    public static String BearerToken(String token) {
        if (IsBearerToken(token)) {
            return token;
        }
        return BEARER + SPACE + token;
    }

    public static Object SafeJSonReader(Gson gson, String json, Class<?> clazz, Object defaultValue) {
        Object o = SafeJSonReader(gson, json, clazz);
        if (o == null) {
            return defaultValue;
        }
        return o;
    }

    public static boolean IsNull(Object o) {
        try {
            for (Field f : Fields(o.getClass())) {
                f.setAccessible(true);
                Object data = f.get(o);
                if (f.get(o) != null && !StringUtils.isEmpty(data))
                    return false;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean FieldsNotNullRequired(Object o, String... fields) {
        List<String> required = Arrays.asList(fields);
        try {
            for (Field f : Fields(o.getClass())) {
                f.setAccessible(true);
                Object data = f.get(o);
                if ((required.isEmpty() || required.contains(f.getName())) && (data == null || StringUtils.isEmpty(data)))
                    return false;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean FieldsNotNullExcept(Object o, String... fields) {
        List<String> list = Arrays.asList(fields);
        try {
            for (Field f : Fields(o.getClass())) {
                if (list.contains(f.getName())) {
                    continue;
                }
                f.setAccessible(true);
                Object data = f.get(o);
                if (data == null || StringUtils.isEmpty(data))
                    return false;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean FieldsNotNull(Object o) {
        try {
            for (Field f : Fields(o.getClass())) {
                f.setAccessible(true);
                Object data = f.get(o);
                if (data == null || StringUtils.isEmpty(data))
                    return false;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static Set<Field> Fields(Class<?> current) {
        Set<Field> fields = Arrays.stream(current.getDeclaredFields()).collect(Collectors.toSet());
        while (current.getSuperclass() != null) {
            fields.addAll(Objects.requireNonNull(Fields(current.getSuperclass())));
            current = current.getSuperclass();
        }
        return fields;
    }

    public static String LastFieldName(Path path) {
        String field = "";
        for (Path.Node node : path) {
            field = node.getName();
        }
        return field;
    }

    public static Collection<File> ListFilesForFolder(final File folder) {
        List<File> files = new ArrayList<>();
        File[] list = folder.listFiles();
        if (list == null) {
            return files;
        }
        for (final File file : list) {
            if (file.isDirectory()) {
                files.addAll(ListFilesForFolder(file));
            } else {
                files.add(file);
            }
        }
        return files;
    }

    public static boolean isWrapperType(Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    private static Set<Class<?>> getWrapperTypes() {
        Set<Class<?>> ret = new HashSet<>();
        ret.add(Boolean.class);
        ret.add(String.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        ret.add(Instant.class);
        ret.add(LocalDateTime.class);
        return ret;
    }

    public static String getClientIpAddressIfServletRequestExist() {

        if (RequestContextHolder.getRequestAttributes() == null) {
            return "0.0.0.0";
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && ipList.length() != 0 && !"unknown".equalsIgnoreCase(ipList)) {
                return ipList.split(COMMA)[0];
            }
        }

        return request.getRemoteAddr();
    }
}




