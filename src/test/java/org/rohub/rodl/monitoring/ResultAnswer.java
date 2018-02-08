package org.rohub.rodl.monitoring;


import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.rohub.rodl.monitoring.ChecksumVerificationJob.Result;

/**
 * An implementation of Mockito's Answer class that sets the job result.
 * 
 * @author piotrekhol
 * 
 */
final class ResultAnswer implements Answer<Void> {

    /** The job result. */
    private Result result;


    @Override
    public Void answer(InvocationOnMock invocation)
            throws Throwable {
        result = (Result) invocation.getArguments()[0];
        return null;
    }


    public Result getResult() {
        return result;
    }
}
