package com.intel.networking.sink.zmq;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import org.zeromq.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class NetworkDataSinkZMQ implements NetworkDataSink {
    public NetworkDataSinkZMQ(Logger logger, Map<String, String> args) {
        setLogger(logger);
        if(args == null) {
            log_.error("No args were given!");
            throw new IllegalArgumentException("args");
        }
        if(!args.containsKey("uri") || args.get("uri") == null ||
                args.get("uri").trim().equals("")) {
            log_.error("No jeroMQ 'uri' was given in the argument map!");
            throw new IllegalArgumentException("args");
        }
        connectionString_ = args.getOrDefault("uri", "tcp://127.0.0.1:5401");
        if (args.containsKey("subjects") && args.get("subjects") != null &&
                !args.get("subjects").trim().equals("")) {
            String[] subjects = args.get("subjects").split(",");
            Collections.addAll(subjects_, subjects);
        } else {
            error("No RabbitMQ 'subjects' was given in the argument map!");
            throw new IllegalArgumentException("args");
        }
    }

    @Override
    public void initialize() { }

    @Override
    public void clearSubjects() {
        subjects_.clear();
    }

    @Override
    public void setMonitoringSubject(String subject) {
        subjects_.add(subject);
    }

    @Override
    public void setMonitoringSubjects(Collection<String> subjects) {
        subjects_.addAll(subjects);
    }

    private void error(String msg) {
        if(log_ != null) log_.error(msg);
    }

    void error(Exception e) {
        error("*** " + e.getMessage());
        for(StackTraceElement trace: e.getStackTrace())
            error(trace.toString());
    }

    @Override
    public void setConnectionInfo(String info) {
        connectionString_ = info;
    }

    @Override
    public void setCallbackDelegate(NetworkDataSinkDelegate delegate) {
        delegate_ = delegate;
    }

    @Override
    public void setLogger(Logger logger) {
        log_ = logger;
    }

    @Override
    public String getProviderName() {
        return "zmq";
    }

    @Override
    public void startListening() {
        listenThread = new Thread(() -> {
            synchronized (this) {
                this.listenThreadStarted = true;
                this.notify();
            }
            try {
                listen();
            }
            catch (Exception e) {
                log_.exception(e,"Could not start a thread.");
            }
        });
        listenThread.start();
        waitThread();
    }

    private synchronized void waitThread() {
        while (!listenThreadStarted) {
            try {
                this.wait();
            } catch (InterruptedException e) { /* Do Nothing */ }
        }
    }

    private ZSocket setUpChannel() throws Exception {
        ZSocket subscriber = createSocketZMQ();
        for (String connection: subjects_) {
            subscriber.subscribe(connection);
        }
        for (String connection: connectionString_.split(",")) {
            if(!subscriber.connect(connection)) {
                closeConnection(subscriber);
                throw new Exception("Failed to connect to event publisher: "+ connection);
            }
        }
        return subscriber;
    }

     ZSocket createSocketZMQ() throws Exception {
        try {
            return new ZSocket(SocketType.SUB.type());
        }
        catch (Exception e) {
            throw new Exception("Unable to create the socket.");
        }

    }

    private void listen() throws Exception {
        ZSocket subscriber = setUpChannel();
        while(!Thread.currentThread().isInterrupted()) {
            String group = subscriber.receiveStringUtf8();
            String payload = subscriber.receiveStringUtf8();
            try {
                if (delegate_ != null)
                    delegate_.processIncomingData(group, payload);
            } catch(Exception e) {
                log_.exception(e, "Callback threw an exception!");
            }
        }
    }

    @Override
    public void stopListening() {
        if(listenThread != null)
            listenThread.interrupt();
    }

    @Override
    public boolean isListening() {
        if(listenThread != null)
            return listenThread.isAlive();
        return false;
    }

    private void closeConnection(ZSocket subscriber) {
        try {
            subscriber.close();
        } catch(Exception e2) { /* Do Nothing...*/ }
    }

    private HashSet<String> subjects_ = new HashSet<>();
    Thread listenThread = null;
    private Logger log_ = null;
    private String connectionString_;
    private NetworkDataSinkDelegate delegate_ = null;
    private boolean listenThreadStarted = false;
}
