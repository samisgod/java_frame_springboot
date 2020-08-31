package d1.framework.webapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.aspectj.util.FileUtil;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class HMACSignService<T> extends DoServiceImpBase {
    private File hmacFile;

    public void verfiySign(HttpServletRequest request, Object obj) throws Exception {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.contains("sign ") || auth.split(" ").length < 2)
            throw new Exception("HMAC的Authorization必须有值，且格式应该是sign xxxx");
        String sign = auth.split(" ")[1];

        String appId = request.getHeader("appid");
        if (StringUtils.isEmpty(appId))
            throw new Exception("HMAC的header不能缺少appid参数");

        String timestamp = request.getHeader("timestamp");
        if (StringUtils.isEmpty(timestamp))
            throw new Exception("HMAC的header不能缺少timestamp参数");

        String[] methods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
        String method = request.getMethod();
        if (!Arrays.asList(methods).contains(method)) {
            throw new Exception("HMAC只支持GET、POST、PUT、DELETE、OPTIONS");
        }

        String appkey = null;
        if (hmacFile == null) {
            hmacFile = ResourceUtils.getFile("classpath:hmac.json");
            if (hmacFile == null || !hmacFile.exists()) {
                throw new Exception("没找到对应的hmac配置文件");

            }
            String content = new String(FileUtil.readAsByteArray(hmacFile), "utf-8");
            JSONArray array = JSONArray.parseArray(content);
            appkey = getObjectByKey(array, appId);
            if (appkey == null) {
                throw new Exception("没找到对应的appid:" + appId);
            }
        }

        Map<String, Object> maps = objectToMapWithoutNull(obj);
        maps.put("appid", appId);
        maps.put("timestamp", timestamp);
        String computeSign = hmacSign(maps, "key", appkey);
        if (!computeSign.equals(sign)) {
            throw new Exception("HMAC签名验证失败");
        }
    }

    private String hmacSign(Map<String, Object> maps, String keyName, String keyValue) throws UnsupportedEncodingException {
        List<Map.Entry<String, Object>> list = new ArrayList<Map.Entry<String, Object>>(maps.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Object>>() {
            @Override
            public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
                // 升序排序
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : list) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        sb.append(keyName + "=" + keyValue);
        String temp = md5_32(sb.toString(), true);
        return temp;
    }

    private String md5_32(String source, boolean isUpper) throws UnsupportedEncodingException {
        //定义一个字节数组
        byte[] secretBytes = null;
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            //对字符串进行加密
            md.update(source.getBytes());
            //获得加密后的数据
            secretBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有md5这个算法！");
        }
        //将加密后的数据转换为16进制数字
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        if (isUpper)
            md5code = md5code.toUpperCase();
        else
            md5code = md5code.toLowerCase();
        return md5code;
    }

    private String getObjectByKey(JSONArray array, String appid) {
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (obj.getString("appid").equals(appid)) {
                return obj.getString("appkey");
            }
        }
        return null;
    }

    private Map<String, Object> objectToMap(Object obj, boolean includeNull) throws Exception {
        if (obj == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<String, Object>();

        List<Field> declaredFields = getParentAndMyFields(obj.getClass());
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if (includeNull || field.get(obj) != null)
                map.put(field.getName(), field.get(obj));
        }
        return map;
    }

    private List<Field> getParentAndMyFields(Class tempClass) {
        List<Field> fieldList = new ArrayList<>();
        while (tempClass != null && !tempClass.getName().toLowerCase().equals("java.lang.object")) {
            fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
            tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
            if (tempClass.getName().toLowerCase().equals("java.lang.object")) break;
        }
        return fieldList;
    }

    //对象转Map,如果值为null，则不包含
    private Map<String, Object> objectToMapWithoutNull(Object obj) throws Exception {
        return objectToMap(obj, false);
    }

    //对象转Map,如果值为null也包含
    private Map<String, Object> objectToMapWithNull(Object obj) throws Exception {
        return objectToMap(obj, true);
    }
}
