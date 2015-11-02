package org.copperengine.core.persistent.cassandra.loadtest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.copperengine.core.WorkflowInstanceDescr;

public class PermanentLoadCreator {

    private static final String WF_CLASS = "org.copperengine.core.persistent.cassandra.loadtest.workflows.LoadTestWorkflow";

    private CassandraEngineFactory factory;
    private final AtomicInteger counter = new AtomicInteger();
    private final String payload;

    public PermanentLoadCreator(int payloadSize) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < payloadSize; i++) {
            sb.append(i % 10);
        }
        payload = sb.toString();
    }

    public synchronized PermanentLoadCreator start() throws Exception {
        if (factory != null)
            return this;

        factory = new CassandraEngineFactory();
        factory.createEngine(false);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                factory.destroyEngine();
            }
        });
        return this;
    }

    public PermanentLoadCreator startThread() {
        new Thread() {
            @Override
            public void run() {
                for (;;) {
                    work();
                }
            }
        }.start();
        return this;
    }

    public void work() {
        try {
            List<String> cids = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                final String cid = factory.engine.createUUID();
                final LoadTestData data = new LoadTestData(cid, payload);
                final WorkflowInstanceDescr<LoadTestData> wfid = new WorkflowInstanceDescr<LoadTestData>(WF_CLASS, data, cid, 1, null);
                factory.engine.run(wfid);
                cids.add(cid);
            }
            for (String cid : cids) {
                factory.backchannel.wait(cid, 5, TimeUnit.MINUTES);
                int value = counter.incrementAndGet();
                if (value % 100 == 0) {
                    System.out.println(new Date() + " - " + value + " workflow instances processed so far.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            new PermanentLoadCreator(4096).start().startThread().startThread().startThread();
            System.out.println("Started!");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}