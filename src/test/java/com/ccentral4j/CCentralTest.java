package com.ccentral4j;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.requests.EtcdKeyPutRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class CCentralTest {

  private CCClient cCentral;
  @Mock
  private EtcdClient client;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    cCentral = new CCEtcdClient("service", client);
    when(client.put(anyString(), anyString())).thenReturn(mock(EtcdKeyPutRequest.class));
  }

  /**
   * Schema is sent on first refresh
   */
  @Test
  public void sendSchema() throws Exception {
    cCentral.refresh();
    verify(client).put("/ccentral/services/service/schema",
        "{\"v\":{\"key\":\"v\",\"title\":\"Version\",\"description\":\"Schema version for tracking instances\",\"type\":\"integer\",\"default\":\"0\"}}");
  }
}