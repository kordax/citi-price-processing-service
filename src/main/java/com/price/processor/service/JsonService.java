package com.price.processor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.price.processor.model.dto.json.JsonExchEntry;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JsonService {
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public List<JsonExchEntry> readJsonExchangeEntries(Resource resource) throws IOException {
      InputStreamReader reader = new InputStreamReader(resource.getInputStream());
      return Arrays.asList((gson.fromJson(reader, JsonExchEntry[].class)));
  }
}
