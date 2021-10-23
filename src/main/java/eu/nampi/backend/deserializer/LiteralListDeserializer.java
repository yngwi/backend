package eu.nampi.backend.deserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.jena.rdf.model.Literal;
import eu.nampi.backend.converter.StringToLiteralConverter;

public class LiteralListDeserializer extends StdDeserializer<List<Literal>> {

  private static final long serialVersionUID = 1L;

  public LiteralListDeserializer() {
    super(String.class);
  }

  private static final TypeReference<List<String>> LIST_OF_STRINGS_TYPE =
      new TypeReference<List<String>>() {};

  @Override
  public List<Literal> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    List<Literal> resources = new ArrayList<>();
    JsonToken token = p.currentToken();
    if (JsonToken.START_ARRAY.equals(token)) {
      List<String> tokens = p.readValueAs(LIST_OF_STRINGS_TYPE);
      resources.addAll(tokens
          .stream()
          .map(s -> new StringToLiteralConverter().convert(s))
          .collect(Collectors.toList()));
    }
    return resources;
  }
}
