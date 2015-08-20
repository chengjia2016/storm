package org.apache.storm.elasticsearch.bolt;

import java.util.Collections;
import java.util.UUID;

import org.apache.storm.elasticsearch.ElasticsearchGetRequest;
import org.apache.storm.elasticsearch.common.EsConfig;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EsLookupBoltTest extends AbstractEsBoltTest<EsLookupBolt> {

    @Mock
    private EsConfig esConfig;

    @Mock
    private ElasticsearchGetRequest getRequest;

    @Mock
    private EsLookupResultOutput output;

    @Mock
    private Tuple tuple;

    @Mock
    private GetRequest request;

    @Mock
    private Client client;

    private Client originalClient;

    @Override
    protected EsLookupBolt createBolt(EsConfig esConfig) {
        originalClient = EsLookupBolt.getClient();
        EsLookupBolt.replaceClient(this.client);
        return new EsLookupBolt(esConfig, getRequest, output);
    }

    @After
    public void replaceClientWithOriginal() throws Exception {
        EsLookupBolt.replaceClient(originalClient);
    }

    @Before
    public void configureBoltDependencies() throws Exception {
        when(getRequest.extractFrom(tuple)).thenReturn(request);
        when(output.toValues(any(GetResponse.class))).thenReturn(Collections.singleton(new Values("")));
    }

    @Test
    public void failsTupleWhenClientThrows() throws Exception {
        when(client.get(request)).thenThrow(ElasticsearchException.class);
        bolt.execute(tuple);

        verify(outputCollector).fail(tuple);
    }

    @Test
    public void reportsExceptionWhenClientThrows() throws Exception {
        ElasticsearchException elasticsearchException = new ElasticsearchException("dummy");
        when(client.get(request)).thenThrow(elasticsearchException);
        bolt.execute(tuple);

        verify(outputCollector).reportError(elasticsearchException);
    }

    @Test
    public void fieldsAreDeclaredThroughProvidedOutput() throws Exception {
        Fields fields = new Fields(UUID.randomUUID().toString());
        when(output.fields()).thenReturn(fields);
        OutputFieldsDeclarer declarer = mock(OutputFieldsDeclarer.class);
        bolt.declareOutputFields(declarer);

        ArgumentCaptor<Fields> declaredFields = ArgumentCaptor.forClass(Fields.class);
        verify(declarer).declare(declaredFields.capture());

        assertThat(declaredFields.getValue(), is(fields));
    }
}
