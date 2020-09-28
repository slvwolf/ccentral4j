package io.github.slvwolf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

class SchemaItem {
  public String key;
  public String title;
  public String description;
  @JsonProperty(value = "default")
  public String defaultValue;
  public String type;
  @JsonIgnore
  public String configValue;
  @JsonIgnore
  private final List<ConfigUpdate> callbacks;

  public enum Type {
    STRING("string"),
    PASSWORD("password"),
    INTEGER("integer"),
    FLOAT("float"),
    LIST("list"),
    BOOLEAN("boolean");
    public final String value;

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
    callbacks = new LinkedList<>();
  }

  public void addCallback(ConfigUpdate func) {
    callbacks.add(func);
  }

  public List<ConfigUpdate> getCallbacks() {
    return callbacks;
  }
}