package com.mytest.events.persistence_service;

/**
 * CountersTableEventHandler handles events for all entities. This is accomplished
 * with the @Asset( "Counters" ) annotation.
 * The methods in the class correspond to the events selected in Backendless
 * Console.
 */
/*
@Asset( "Counters" )
public class CountersTableEventHandler extends com.backendless.servercode.extension.PersistenceExtender<Counters>
{

    @Override
    public void afterFind( RunnerContext context, BackendlessDataQuery query, ExecutionResult<BackendlessCollection<Counters>> result ) throws Exception
    {
        Backendless.Logging.setLogReportingPolicy(AppConstants.LOG_POLICY_NUM_MSGS, AppConstants.LOG_POLICY_FREQ_SECS);
        Logger logger = Backendless.Logging.getLogger("com.mytest.events.CountersTableEventHandler");

        logger.debug("In CountersTableEventHandler: afterFind");

        if (result.getException() == null) {
            BackendlessCollection<Counters> collection = result.getResult();
            if( collection.getTotalObjects() == 1) {
                Counters counter = collection.getData().get(0);
                // increment and save
                counter.setValue(counter.getValue()+1);
                try
                {
                    Backendless.Persistence.save( counter );
                }
                catch( BackendlessException e ) {
                    logger.error("Exception while saving incremented counter value: " + e.toString());
                    BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_GENERAL, "Failed to increment counter value.");
                    throw new BackendlessException(fault);
                }
            } else {
                logger.error("ExecutionResult does not contain exactly one counters object");
                BackendlessFault fault = new BackendlessFault(AppConstants.BL_MYERROR_GENERAL, "More than one return objects are not allowed.");
                throw new BackendlessException(fault);
            }
        }

    }

}
*/