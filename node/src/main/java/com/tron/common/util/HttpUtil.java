package com.tron.common.util;

import static com.tron.common.Constant.HTTP_MAX_RETRY_TIME;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

public class HttpUtil {

  private static HttpClient client = HttpClients.createDefault();

  public static HttpResponse get(String scheme, String host, String path, Map<String, String> paramMap) {
    List<NameValuePair> params = new ArrayList<>();
    paramMap.keySet().forEach(
            k -> {
              params.add(new BasicNameValuePair(k, paramMap.get(k)));
            }
    );
    URI uri = null;
    try {
      uri = new URIBuilder().setScheme(scheme).setHost(host).setPath(path)
              .setParameters(params)
              .build();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    HttpGet httpGet = new HttpGet(uri);
    try {
      int timeout = 5; //second
      RequestConfig config = RequestConfig.custom()
              .setConnectTimeout(timeout * 1000)
              .setConnectionRequestTimeout(timeout * 1000)
              .setSocketTimeout(timeout * 1000).build();
      client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
      return client.execute(httpGet);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static HttpResponse post(String scheme, String host, String path, Map<String, Object> paramMap) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String jsonString = mapper.writeValueAsString(paramMap);
    StringEntity entity = new StringEntity(jsonString, "UTF-8");
    URI uri = null;
    try {
      uri = new URIBuilder()
              .setScheme(scheme)
              .setHost(host)
              .setPath(path)
              .build();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    HttpPost httpPost = new HttpPost(uri);
    httpPost.setEntity(entity);
    httpPost.setHeader("Content-Type", "application/json;charset=utf8");
    try {
      return client.execute(httpPost);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static HttpResponse getByUri(String uriStr) {
    URI uri = null;
    try {
      uri = new URI(uriStr);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    HttpGet httpGet = new HttpGet(uri);
    try {
      return client.execute(httpGet);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static HttpResponse requestWithRetry(String url) {
    try {
      HttpResponse response = getByUri(url);
      if (response == null) {
        return null;
      }
      int status = response.getStatusLine().getStatusCode();
      if (status == HttpStatus.SC_SERVICE_UNAVAILABLE) {
        int retry = 1;
        while (true) {
          if (retry > HTTP_MAX_RETRY_TIME) {
            break;
          }
          try {
            Thread.sleep(100 * retry);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          response = HttpUtil.getByUri(url);
          if (response == null) {
            break;
          }
          retry++;
          status = response.getStatusLine().getStatusCode();
          if (status != HttpStatus.SC_SERVICE_UNAVAILABLE) {
            break;
          }
        }
      }
      return response;
    } catch (Exception e) {
      return null;
    }
  }

}
