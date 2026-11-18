package com.example.desktop.api;

import com.example.desktop.dto.ProductDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public class PackingApi extends StationApi
{
  public PackingApi(String baseUrl)
  {
    super(baseUrl);
  }

  public List<ProductDTO> products()
  {
    return rest.get("/products", new TypeReference<List<ProductDTO>>() {});
  }

  public ProductDTO pack(ProductDTO product)
  {
    return rest.post("/products", product, new TypeReference<ProductDTO>() {});
  }
}
