package com.ccentral4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SchemaItem {
  public String key;
  public String title;
  public String description;
  @JsonProperty(value = "default")
  public String defaultValue;
  public String type;
  @JsonIgnore
  public String configValue;

  public SchemaItem(String key, String title, String description, String defaultValue) {
    this.key = key;
    this.title = title;
    this.description = description;
    this.defaultValue = defaultValue;
    type = "string";
    configValue = null;
  }

  public SchemaItem() {
  }
}