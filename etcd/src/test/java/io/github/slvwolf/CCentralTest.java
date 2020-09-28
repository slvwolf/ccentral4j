package io.github.slvwolf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CCentralTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private CCEtcdClient cCentral;
  @Mock
  private EtcdAccess client;
  @Mock
  private Logger logger;
  @Captor
  private ArgumentCaptor<String> stringCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    cCentral = new CCEtcdClient(client);
  }

  /**
   * Schema is sent on first refresh
   */
  @Test
  public void sendSchema() throws Exception {
    cCentral.refresh();
    verify(client).sendSchema(
        "{\"v\":{\"key\":\"v\",\"title\":\"Version\",\"description\":\"Schema version for tracking instances\",\"type\":\"integer\",\"default\":\"0\"}}");
  }

  /** List types, get defaults */
  @Test
  public void getListDefault() {
    cCentral.addListField("list", "title", "description", Collections.singletonList("default"));

    List<String> values = cCentral.getConfigList("list");

    assertThat("Exactly one item in list", values.size(), is(1));
    assertThat("Item should be 'default'", values.get(0), is("default"));
  }

  /** List types, get value */
  @Test
  public void getListValue() throws Exception {
    when(client.fetchConfig()).thenReturn("{\"list\": {\"value\": \"[\\\"current\\\"]\"}}");
    cCentral.addListField("list", "title", "description", Collections.singletonList("default"));

    List<String> values = cCentral.getConfigList("list");

    assertThat("Exactly one item in list", values.size(), is(1));
    assertThat("Item should be 'current'", values.get(0), is("current"));
  }

  /** Bool types, get defaults */
  @Test
  public void getBoolDefault() {
    cCentral.addBooleanField("bool", "title", "description", false);

    assertThat("Result should be false", cCentral.getConfigBool("bool"), is(false));
  }

  /**
   * Bool types, get value
   */
  @Test
  public void getBoolValue() throws Exception {
    when(client.fetchConfig()).thenReturn("{\"bool\": {\"value\": \"1\"}}");
    cCentral.addBooleanField("bool", "title", "description", false);

    assertThat("Result should be true", cCentral.getConfigBool("bool"), is(true));
  }

  /**
   * Provided callback function is called back on configuration change
   */
  @Test
  public void noCallbackOnFirstRun() throws Exception {
    when(client.fetchConfig()).thenReturn("{\"bool\": {\"value\": \"1\"}}");
    ConfigUpdate configUpdate = Mockito.mock(ConfigUpdate.class);
    cCentral.setConfigCheckInterval(-1);
    cCentral.addBooleanField("bool", "title", "description", false);
    cCentral.addCallback("bool", configUpdate);

    cCentral.refresh();

    verifyNoMoreInteractions(configUpdate);
  }

  /**
   * Provided callback function is called back on configuration change
   */
  @Test
  public void callback() throws Exception {
    when(client.fetchConfig()).thenReturn("{\"bool\": {\"value\": \"1\"}}");
    ConfigUpdate configUpdate = Mockito.mock(ConfigUpdate.class);
    cCentral.setConfigCheckInterval(-1);
    cCentral.addBooleanField("bool", "title", "description", false);
    cCentral.addCallback("bool", configUpdate);

    cCentral.refresh();
    reset(client);
    when(client.fetchConfig()).thenReturn("{\"bool\": {\"value\": \"0\"}}");
    cCentral.refresh();

    verify(configUpdate).valueChanged(eq("bool"));
  }

  /**
   * If configuration value has not changed, callback is not called
   */
  @Test
  public void noCallback() throws Exception {
    when(client.fetchConfig()).thenReturn("{\"bool\": {\"value\": \"1\"}}");
    ConfigUpdate configUpdate = Mockito.mock(ConfigUpdate.class);
    cCentral.setConfigCheckInterval(-1);
    cCentral.addBooleanField("bool", "title", "description", false);
    cCentral.addCallback("bool", configUpdate);

    cCentral.refresh();
    when(client.fetchConfig()).thenReturn("{\"bool\": {\"value\": \"1\"}}");
    cCentral.refresh();

    verifyNoMoreInteractions(configUpdate);
  }

  /**
   * Throw exception on addCallback if configuration option is missing
   */
  @Test(expected = UnknownConfigException.class)
  public void testMethod() throws Exception {
    cCentral.addCallback("bool", Mockito.mock(ConfigUpdate.class));
  }

  /**
   * Password types, do not log password (verify correct branch)
   */
  @Test
  public void getPasswordValue() throws Exception {
    when(client.fetchConfig()).thenReturn("{\"password_title\": {\"value\": \"pass2\"}}");
    CCEtcdClient.setLogger(logger);
    cCentral.addPasswordField("password_title", "title", "description", "pass1");

    cCentral.refresh();

    verify(logger).info(eq("Configuration value for '{}' changed."), eq("password_title"));
  }

  /**
   * Pull configuration on late field definitions
   */
  @Test
  public void pullConfigLate() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
    cCentral.addField("key", "title", "desc", "def");
    cCentral.refresh();
    reset(client);

    cCentral.addField("key2", "title", "desc", "def");
    verify(client).fetchConfig();
  }

  /** Increment with groups */
  @Test
  public void incGroups() throws Exception {
    cCentral.incrementInstanceCounter("key", "group1", "group2");
    cCentral.refresh();

    verify(client).sendClientInfo(stringCaptor.capture());
    assertTrue(stringCaptor.getValue().contains("\"c_key.group1.group2\":[0]"));
  }

  /** Group parameters are cleaned */
  @Test
  public void cleanGroups() throws Exception {
    cCentral.incrementInstanceCounter("key", "invalid.character", "second character");
    cCentral.refresh();

    verify(client).sendClientInfo(stringCaptor.capture());
    assertTrue(stringCaptor.getValue().contains("\"c_key.invalidcharacter.second_character\":[0]"));
  }

  /** Increment without groups */
  @Test
  public void incNoGroups() throws Exception {
    cCentral.incrementInstanceCounter("key");
    cCentral.refresh();

    verify(client).sendClientInfo(stringCaptor.capture());
    assertTrue(stringCaptor.getValue().contains("\"c_key\":[0]"));
  }

  @Test
  public void histogram() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
    cCentral.addHistogram("latency", 10);
    cCentral.addHistogram("latency", 12);
    cCentral.addHistogram("latency", 7);
    cCentral.setClock(Clock.offset(cCentral.getClock(), Duration.ofMinutes(1)));
    cCentral.refresh();
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(client, times(2)).sendClientInfo(captor.capture());
    String data = captor.getAllValues().get(1);
    Map<String, Object> values = MAPPER.readValue(data, new TypeReference<Map<String, Object>>() {
    });
    @SuppressWarnings("unchecked")
    List<Double> latencies = (List) values.get("h_latency");
    assertThat(latencies, notNullValue());
    assertThat(latencies, hasItems(12.0, 12.0, 12.0, 10.0));
  }
}