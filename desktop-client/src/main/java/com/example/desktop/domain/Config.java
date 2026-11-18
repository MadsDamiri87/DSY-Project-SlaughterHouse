package com.example.desktop.domain;

public record Config(String registrationUrl,
                     String cuttingUrl,
                     String packingUrl,
                     String backendUrl,
                     String traceHost,
                     int tracePort)
{
  public static Config fromEnvironment()
  {
    return new Config(
        value("app.registration.url", "REGISTRATION_URL", "http://localhost:8081"),
        value("app.cutting.url", "CUTTING_URL", "http://localhost:8082"),
        value("app.packing.url", "PACKING_URL", "http://localhost:8083"),
        value("app.backend.url", "BACKEND_BASE_URL", "http://localhost:8080"),
        value("app.trace.host", "TRACE_HOST", "localhost"),
        Integer.parseInt(value("app.trace.port", "GRPC_PORT", "9090")));
  }

  public String urlFor(Station station)
  {
    return switch (station)
    {
      case REGISTRATION -> registrationUrl;
      case CUTTING -> cuttingUrl;
      case PACKING -> packingUrl;
    };
  }

  private static String value(String systemProperty, String environmentVariable, String fallback)
  {
    String fromProperty = System.getProperty(systemProperty);
    if (fromProperty != null && !fromProperty.isBlank())
    {
      return fromProperty;
    }

    String fromEnvironment = System.getenv(environmentVariable);
    if (fromEnvironment != null && !fromEnvironment.isBlank())
    {
      return fromEnvironment;
    }

    return fallback;
  }
}
