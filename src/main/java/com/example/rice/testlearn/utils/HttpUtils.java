package com.example.rice.testlearn.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * http 工具类
 *
 * @author luowei
 * @date 2018/12/1 10:11
 */
public final class HttpUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    /**
     * 发送http get请求
     *
     * @param url    请求地址
     * @param config 可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static String sendHttpGet(String url, RequestConfig... config) {
        return sendHttpGet(url, null, null, config);
    }

    /**
     * 发送http get请求
     *
     * @param url    请求url地址
     * @param params url参数
     * @param config 可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static String sendHttpGet(String url, Map<String, String> params, RequestConfig... config) {
        return sendHttpGet(url, null, params, config);
    }

    /**
     * 发送http get请求
     *
     * @param url     请求地址
     * @param headers url头文件
     * @param params  url参数
     * @param config  可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static String sendHttpGet(String url, Map<String, String> headers, Map<String, String> params,
                                     RequestConfig... config) {
        // 创建默认的httpClient实例.
        CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(createPoll()).build();
//        CloseableHttpClient httpclient = HttpClients.createDefault() ;
        try {
            HttpResponse response = getHttpResponse(httpclient, url, headers, params, config);
            // 获取响应实体
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
        } catch (Exception e) {
        } finally {
            close(httpclient);
        }
        return null;
    }

    /**
     * 获取Http响应，此方法中不对httpClient进行关闭
     * httpClient需要调用方关闭
     *
     * @param httpClient httpClient
     * @param url        请求地址
     * @param headers    url头文件
     * @param params     url参数
     * @param config     可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static HttpResponse getHttpResponse(HttpClient httpClient, String url, Map<String, String> headers,
                                               Map<String, String> params, RequestConfig... config) {
        try {
            // 创建参数
            URIBuilder builder = null;
            if (params != null) {
                builder = new URIBuilder(url);
                for (Entry<String, String> entry : params.entrySet()) {
                    builder.addParameter(entry.getKey(), entry.getValue());
                }
            }
            // 创建httpget
            HttpGet httpget = builder != null ? new HttpGet(builder.build()) : new HttpGet(url);
            // 创建配置
            httpget.setConfig(config != null && config.length == 1 ? (RequestConfig) config[0] : createDefaultRequestConfig());
            // 创建头文件
            if (headers != null) {
                for (Entry<String, String> entry : headers.entrySet()) {
                    httpget.setHeader(entry.getKey(), entry.getValue());
                }
            }
            // 执行get请求.
            return httpClient.execute(httpget);

        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 获取http响应
     *
     * @param url
     * @return
     */
    public static HttpResponse getHttpResponse(String url) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            // 执行get请求.
            return getHttpResponse(httpclient, url, null, null);
        } finally {
            close(httpclient);
        }
    }

    /**
     * 发送http post请求
     *
     * @param url    请求地址
     * @param params url参数
     * @param config 可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static String sendHttpPost(String url, Map<String, String> params, RequestConfig... config) {
        return sendHttpPost(url, null, params, config);
    }

    /**
     * 发送http post请求
     *
     * @param url    请求地址
     * @param params url参数
     * @param config 可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static String sendHttpPost(String url, String params, RequestConfig... config) {
        return sendHttpPost(url, params.getBytes(Charset.forName("UTF-8")), config);
    }

    /**
     * 发送http post请求
     *
     * @param url    请求地址
     * @param params url参数
     * @param config 可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static String sendHttpPost(String url, byte[] params, RequestConfig... config) {
        return sendHttpPost(url, null, params, config);
    }

    /**
     * 发送http post请求
     *
     * @param url     请求地址
     * @param headers url头文件
     * @param params  url参数
     * @param config  可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static String sendHttpPost(String url, Map<String, String> headers, Map<String, String> params, RequestConfig... config) {
        // 创建参数列表
        List<NameValuePair> bodys = new ArrayList<>();
        if (params != null) {
            for (Entry<String, String> entry : params.entrySet()) {
                bodys.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        return sendHttpPost(url, headers, new UrlEncodedFormEntity(bodys, Charset.forName("UTF-8")), config);
    }

    /**
     * 发送http post请求
     *
     * @param url     请求地址
     * @param headers url头文件
     * @param params  url参数
     * @param config  可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    public static String sendHttpPost(String url, Map<String, String> headers, byte[] params, RequestConfig... config) {
        return sendHttpPost(url, headers, new ByteArrayEntity(params), config);
    }

    /**
     * 发送http请求
     *
     * @param url        请求地址
     * @param headers    url头文件
     * @param httpEntity http参数entity
     * @param config     可选参数，不设置将使用默认connecttimeout:1000;sockettimeout:3000
     * @return
     */
    private static String sendHttpPost(String url, Map<String, String> headers, HttpEntity httpEntity, RequestConfig... config) {
        // 创建默认的httpClient实例.
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            // 创建httppost
            HttpPost httppost = new HttpPost(url);
            // 创建头文件
            if (headers != null) {
                for (Entry<String, String> entry : headers.entrySet()) {
                    httppost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            // 创建配置
            httppost.setConfig(config != null && config.length == 1 ? (RequestConfig) config[0] :
                    createDefaultRequestConfig());
            httppost.setEntity(httpEntity);
            // 执行post请求.
            CloseableHttpResponse response = httpclient.execute(httppost);
            // 获取响应实体
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
        } catch (Exception e) {
        } finally {
            close(httpclient);
        }

        return null;
    }

    /**
     * 拼接请求参数，按照aa=aa&bb=bb格式拼接,参数名ASCII码从小到大排序（字典序）
     *
     * @param reqParams   请求参数
     * @param charsetName 编码名, 如果为null,则使用默认编码集
     * @return
     */
    public static String spliceReqParams(Map<String, String> reqParams, String charsetName) {
        // key排序.参数名ASCII码从小到大排序（字典序）
        List<String> keys = new ArrayList<>(reqParams.keySet());
        Collections.sort(keys);
        StringBuilder signInfo = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = reqParams.get(key);
            signInfo.append(buildKeyValue(key, value, charsetName));
            if (i != keys.size() - 1) {
                signInfo.append("&");
            }
        }

        return signInfo.toString();
    }

    /**
     * 拼接键值对.构建key=value格式字符串返回
     *
     * @param key         指定key
     * @param value       指定值
     * @param charsetName 编码名
     * @return "key=value"
     */
    private static String buildKeyValue(String key, String value, String charsetName) {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        sb.append("=");
        if (charsetName != null) {
            try {
                sb.append(URLEncoder.encode(value, charsetName));
            } catch (UnsupportedEncodingException e) {
                sb.append(value);
            }
        } else {
            sb.append(value);
        }
        return sb.toString();
    }

    /**
     * 创建默认请求配置
     *
     * @return
     */
    private static RequestConfig createDefaultRequestConfig() {
        return RequestConfig.custom()
                .setSocketTimeout(3000)
                .setConnectTimeout(1000)
                .build();
    }

    /**
     * 创建请求配置参数
     *
     * @param connectTimeout
     * @param socketTimeout
     * @return
     */
    public static RequestConfig createRequestConfig(int connectTimeout, int socketTimeout) {
        return RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
    }

    /**
     * 关闭连接
     *
     * @param httpClient http连接对象
     */
    public static void close(Closeable httpClient) {
        if (httpClient == null) {
            return;
        }

        try {
            httpClient.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static PoolingHttpClientConnectionManager createPoll() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(40);
        cm.setDefaultMaxPerRoute(40);
        return cm;
    }

}
