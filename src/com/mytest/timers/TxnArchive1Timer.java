//package com.mytest.timers;

//import com.backendless.servercode.annotation.BackendlessTimer;

/**
 * TxnArchive0Timer is a timer.
 * It is executed according to the schedule defined in Backendless Console. The
 * class becomes a timer by extending the TimerExtender class. The information
 * about the timer, its name, schedule, expiration date/time is configured in
 * the special annotation - BackendlessTimer. The annotation contains a JSON
 * object which describes all properties of the timer.
 */
/*
@BackendlessTimer("{'startDate':1463167800000,'frequency':{'schedule':'daily','repeat':{'every':1}},'timername':'TxnArchive1'}")
public class TxnArchive1Timer extends com.backendless.servercode.extension.TimerExtender
{
    private String MERCHANT_ID_SUFFIX = "1";

    @Override
    public void execute( String appVersionId ) throws Exception
    {
        TxnArchiver archiver = new TxnArchiver(MERCHANT_ID_SUFFIX);
        archiver.execute();
    }
}*/