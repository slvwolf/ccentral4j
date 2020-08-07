package com.ccentral4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

class SchemaItem {
  public String key;
  public String title;
  public String description;
  @JsonProperty(value = "default")
  public String defaultValue;
  public String type;
  @JsonIgnore
  public String configValue;

  public enum Type {
    STRING("string"), PASSWORD("password"), INTEGER("integer"), FLOAT("float"), LIST("list"), BOOLEAN("boolean");

    private final String value;

    Type(String type) {
      value = type;
    }
  }

  public SchemaItem(String key, String title, String description, String defaultValue, Type type) {
    this.key = key;
    this.title = title;
    this.description = description;
    this.defaultValue = defaultValue;
    this.type = type.value;
    configValue = null;
  }

  public SchemaItem() {
  }
}