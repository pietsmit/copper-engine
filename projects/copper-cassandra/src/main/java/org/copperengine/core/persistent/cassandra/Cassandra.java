package org.copperengine.core.persistent.cassandra;

import java.util.Iterator;

import org.copperengine.core.Response;

public interface Cassandra {

    public void safeWorkflowInstance(CassandraWorkflow cw) throws Exception;

    public void deleteWorkflowInstance(String wfId) throws Exception;

    public CassandraWorkflow readCassandraWorkflow(String wfId) throws Exception;

    public Iterator<CassandraWorkflow> readAllWorkflowInstances() throws Exception;

    public void safeEarlyResponse(Response<?> r) throws Exception;

    public Response<?> readEarlyResponse(String cid) throws Exception;

    public void deleteEarlyResponse(String cid) throws Exception;

}
