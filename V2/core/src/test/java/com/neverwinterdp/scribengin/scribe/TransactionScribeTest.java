package com.neverwinterdp.scribengin.scribe;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

import com.neverwinterdp.scribengin.Record;
import com.neverwinterdp.scribengin.dataflow.DataflowContainer;
import com.neverwinterdp.scribengin.dataflow.DataflowTaskContext;
import com.neverwinterdp.scribengin.dataflow.DataflowTaskDescriptor;
import com.neverwinterdp.scribengin.dataflow.DataflowTaskReport;
import com.neverwinterdp.scribengin.dataflow.DataflowTaskUnitTest.TestCopyDataProcessor;
import com.neverwinterdp.scribengin.sink.Sink;
import com.neverwinterdp.scribengin.sink.SinkDescriptor;
import com.neverwinterdp.scribengin.source.Source;
import com.neverwinterdp.scribengin.source.SourceDescriptor;
import com.neverwinterdp.testSink.TestSink;
import com.neverwinterdp.testSource.TestSource;

public class TransactionScribeTest {
  @Test
  public void TestTransactionScribe() throws Exception{
    SimpleCopyScribe scribe = new SimpleCopyScribe();
    assertEquals(scribe.getState(), ScribeState.INIT);
    
    
    Source source = new TestSource(new SourceDescriptor("Test")) ;
    Sink sink = new TestSink(new SinkDescriptor("Test"));
    Sink invalidSink = new TestSink(new SinkDescriptor("Test"));
    
    DataflowContainer dataflowContainer = new DataflowContainer(new HashMap<String, String>());
    DataflowTaskDescriptor descriptor = new DataflowTaskDescriptor();
    DataflowTaskReport report = new DataflowTaskReport();
    
    descriptor.setId(0);
    descriptor.setDataProcessor(TestCopyDataProcessor.class.getName());
    descriptor.setSourceStreamDescriptor(source.getStream(0).getDescriptor());
    descriptor.add("default", sink.newStream().getDescriptor());
    descriptor.add("invalid", invalidSink.newStream().getDescriptor());
    
    
    DataflowTaskContext ctx = new DataflowTaskContext(dataflowContainer,
                                                       descriptor,
                                                       report);
    
    for(int i=0; i< 1000; i++){
      Record r = new Record(Integer.toString(i), Integer.toString(i).getBytes());
      scribe.process(r, ctx);      
    }
  }
}
