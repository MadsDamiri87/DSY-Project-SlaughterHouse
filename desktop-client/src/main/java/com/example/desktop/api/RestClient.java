package com.example.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RestClient
{
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final HttpClient http;
  private final String baseUrl;

  public RestClient(String baseUrl)
  {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.http = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
  }

  public String baseUrl()
  {
    return baseUrl;
  }

  public <T> T get(String path, TypeReference<T> type)
  {
    HttpRequest request = request(path).GET().build();
    return read(send(request), type);
  }

  public <T> T post(String path, Object body, TypeReference<T> type)
  {
    HttpRequest request = request(path)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(write(body)))
        .build();

    return read(send(request), type);
  }

  public <T> T put(String path, TypeReference<T> type)
  {
    HttpRequest request = request(path)
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.noBody())
        .build();

    return read(send(request), type);
  }

  public boolean reachable()
  {
    try
    {
      HttpRequest request = request("/").GET().timeout(TIMEOUT).build();
      http.send(request, HttpResponse.BodyHandlers.discarding());
      return true;
    }
    catch (IOException exception)
    {
      return false;
    }
    catch (InterruptedException exception)
    {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private HttpRequest.Builder request(String path)
  {
    return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(TIMEOUT);
  }

  private HttpResponse<String> send(HttpRequest request)
  {
    try
    {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() >= 400)
      {
        throw new ApiException(response.statusCode(), detailOf(response.body()));
      }

      return response;
    }
    catch (IOException exception)
    {
      throw new BackendUnavailableException(
          request.uri().getHost() + ":" + request.uri().getPort() + " svarer ikke", exception);
    }
    catch (InterruptedException exception)
    {
      Thread.currentThread().interrupt();
      throw new BackendUnavailableException("Afbrudt", exception);
    }
  }

  private <T> T read(HttpResponse<String> response, TypeReference<T> type)
  {
    try
    {
      String body = response.body();
      if (body == null || body.isBlank())
      {
        return null;
      }
      return Json.mapper().readValue(body, type);
    }
    catch (Exception exception)
    {
      throw new ApiException(response.statusCode(),
                             "Kunne ikke læse svaret: " + exception.getMessage());
    }
  }

  private String write(Object body)
  {
    try
    {
      return Json.mapper().writeValueAsString(body);
    }
    catch (Exception exception)
    {
      throw new IllegalArgumentException("Kunne ikke skrive forespørgslen", exception);
    }
  }

  private String detailOf(String body)
  {
    if (body == null || body.isBlank())
    {
      return "Ukendt fejl";
    }

    try
    {
      JsonNode node = Json.mapper().readTree(body);
      JsonNode detail = node.get("detail");
      if (detail != null && !detail.isNull())
      {
        return detail.asText();
      }
      JsonNode message = node.get("message");
      if (message != null && !message.isNull())
      {
        return message.asText();
      }
    }
    catch (Exception exception)
    {
      return body;
    }

    return body;
  }
}
