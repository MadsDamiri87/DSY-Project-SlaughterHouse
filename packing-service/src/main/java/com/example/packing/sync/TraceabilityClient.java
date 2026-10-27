package com.example.packing.sync;

import com.example.packing.dto.ProductDTO;
import com.example.packing.outbox.OutboxEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class TraceabilityClient
{
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public TraceabilityClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
                            @Value("${app.backend.base-url}") String baseUrl)
  {
    this.restClient   = restClientBuilder.baseUrl(baseUrl).build();
    this.objectMapper = objectMapper;
  }

  public void send(OutboxEntry entry)
  {
    switch (entry.getType())
    {
      case PRODUCT_PACKED -> post("/products", read(entry, ProductDTO.class));
      default -> throw new BackendRejectedException("Unsupported event type " + entry.getType());

    }
  }

  private void post(String path, Object body)
  {
    try
    {
      restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                  throw new BackendRejectedException(
                      "Backend responded at " + response.getStatusCode() + " for " + path);
                }).onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                  throw new BackendUnavailableException(
                      "Backend responded " + response.getStatusCode() + " for " + path);
                }).toBodilessEntity();
    }
    catch (ResourceAccessException exception)
    {
      throw new BackendUnavailableException("Backend could not be reached at " + path, exception);
    }
  }

  private <T> T read(OutboxEntry entry, Class<T> type)
  {
    try
    {
      return objectMapper.readValue(entry.getPayload(), type);
    }
    catch (JsonProcessingException exception)
    {
      throw new BackendRejectedException(
          "Event: " + entry.getEventId() + " has an unreadable payload ", exception);
    }
  }


}
