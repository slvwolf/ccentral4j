package com.ccentral4j;

import junit.framework.TestCase;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.requests.EtcdKeyPutRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class CCentralTest extends TestCase {

  private CCentral cCentral;
  @Mock
  private EtcdClient client;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    cCentral = new CCentral("service", client);
    when(client.put(anyString(), anyString())).thenReturn(mock(EtcdKeyPutRequest.class));
  }

  /**
   * Schema is sent on first refresh
   */
  @Test
  public void testSendSchema() throws Exception {
    cCentral.refresh();
    verify(client).put("/ccentral/services/service/schema",
        "{\"v\":{\"key\":\"v\",\"title\":\"Version\",\"description\":\"Schema version for tracking instances\",\"type\":\"string\",\"default\":\"default\"}}");
  }
}