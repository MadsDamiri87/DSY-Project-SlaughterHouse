package com.example.shared.messaging;

public final class MessagingTopology
{
  public static final String ANIMAL_EXCHANGE = "slaughterhouse.animals";
  public static final String ANIMAL_REGISTERED_ROUTING_KEY = "animal.registered";
  public static final String ANIMAL_REGISTRATION_QUEUE = "traceability.animal-registered";

  public static final String DEAD_LETTER_EXCHANGE = "slaughterhouse.animals.dlx";
  public static final String ANIMAL_REGISTRATION_DEAD_LETTER_QUEUE =
      "traceability.animal-registered.dlq";

  private MessagingTopology()
  {
  }
}
