package com.intel.dai.eventsim;

import com.intel.logging.Logger;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.IOException;

public class WlmApi {

    Logger logger_;
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String bgschedPath = "/var/log/bgsched.log";
    String cqmPath = "/var/log/cqm.log";

    public WlmApi(Logger logger) {
        logger_ = logger;
    }

    public void createReservation(String name, String users, String nodes, Timestamp starttime, String duration) throws IOException {

        logger_.info("Generate create reservation log line");
        FileWriter bgschedLog = new FileWriter(bgschedPath);
        String[] userArr = users.split(" ");
        String[] nodeArr = nodes.split(" ");

        Reservation reservation = new Reservation(name, userArr, nodeArr, starttime.toString(), Long.valueOf(duration));
        String logstring = reservation.createReservation();

        bgschedLog.write(logstring);
        bgschedLog.close();
    }

    public void modifyReservation(String name, String users, String nodes, String starttime) throws IOException {

        logger_.info("Generate modify reservation log line");
        FileWriter bgschedLog = new FileWriter(bgschedPath);
        String updateStr = "[{";
        if (!"false".equals(users))
            updateStr += "'users': '" + users.replace(",",":").replace(" ",":") + "',";
        if (!"false".equals(nodes))
            updateStr += "'nodes': '" + nodes.replace(" ",",")  + "',";
        if (!"false".equals(starttime))
            updateStr += "'start': '" + Timestamp.valueOf(starttime).toString() + "',";
        updateStr += "}]";

        Reservation reservation = new Reservation(name, null, null, null, 0);
        String logstring = reservation.modifyReservation(updateStr);

        bgschedLog.write(logstring);
        bgschedLog.close();
    }

    public void deleteReservation(String name) throws IOException {

        logger_.info("Generate delete reservation log line");
        FileWriter bgschedLog = new FileWriter(bgschedPath);

        Reservation reservation = new Reservation(name, null, null, null, 0);
        String logstring = reservation.deleteReservation();

        bgschedLog.write(logstring);
        bgschedLog.close();
    }


    public void startJob(String jobid, String name, String users, String nodes, Timestamp starttime, String workdir) throws IOException {

        logger_.info("Generate start job log line");
        FileWriter cqmLog = new FileWriter(cqmPath);
        String[] userArr = users.split(" ");
        String[] nodeArr = nodes.split(" ");

        Job job_ = new Job(Integer.valueOf(jobid), name, nodeArr, userArr, starttime.getTime(), 0, workdir);
        String logstring = job_.startJob();

        cqmLog.write(logstring);
        cqmLog.close();
    }

    public void terminateJob(String jobid, String name, String users, String nodes, Timestamp starttime, String workdir, String exitStatus) throws IOException {

        logger_.info("Generate terminate job log line");
        FileWriter cqmLog = new FileWriter(cqmPath);
        Date now = new Date();
        long endTime = now.getTime();
        String[] userArr = users.split(" ");
        String[] nodeArr = nodes.split(" ");

        Job job_ = new Job(Integer.valueOf(jobid), name, nodeArr, userArr, starttime.getTime(), endTime, workdir);
        String logstring = job_.terminateJob(Integer.valueOf(exitStatus));

        cqmLog.write(logstring);
        cqmLog.close();
    }

    public void simulateWlm(String reservations, String[] nodes) throws Exception {

        HashMap<String, Reservation> logItems = new HashMap<String, Reservation>();
        String[] users = "user1 user2 user3 user4 user5 user6 user 7 user8 user9 user10".split(" ");
        Random rand = new Random();
        int numRes = Integer.parseInt(reservations);
        String[] reservedNodes, choices, id;
        Date now;
        String starttime, logstring, key, nextStep, updateStr;
        Reservation reservation;
        String[] userChoice = new String[2];
        userChoice[0] = "false";
        userChoice[1] = String.join(":", getRandomSubArray(users));
        String[] nodeChoice = new String[2];
        nodeChoice[0] = "false";
        nodeChoice[1] = String.join(",", getRandomSubArray(nodes));
        String[] startChoice = new String[2];
        startChoice[0] = "false";
        startChoice[1] = sdfDate.format(new Date());
        FileWriter cqmLog = new FileWriter(cqmPath);
        FileWriter bgschedLog = new FileWriter(bgschedPath);

        for(int i = 0; i < numRes; i++) {
            int numReservedNodes = rand.nextInt(nodes.length);
            reservedNodes = getRandomSubArray(nodes);
            now = new Date();
            starttime = sdfDate.format(now);
            reservation = new Reservation("reservation" + i, getRandomSubArray(users), reservedNodes, starttime.toString(), rand.nextInt(86400000));
            logItems.put("createRes_" + i, reservation);
        }

        while(!logItems.isEmpty()) {
            Thread.sleep(1000);
            key = logItems.keySet().toArray(new String[0])[rand.nextInt(logItems.size())];
            id = key.split("_");
            logger_.info("Picked item " + key);

            if(id[0].equals("createRes")) {
                logstring = logItems.get(key).createReservation();
                bgschedLog.write(logstring);
                choices = "startJob modifyRes deleteRes".split(" ");
                nextStep = choices[rand.nextInt(choices.length)];
                logItems.put(nextStep + "_" + id[1], logItems.get(key));
                logItems.remove(key);
            }
            if(id[0].equals("modifyRes")) {
                int uc = rand.nextInt(1);
                int nc = rand.nextInt(1);
                int sc = rand.nextInt(1);
                updateStr = "[{";
                if (!"false".equals(userChoice[uc]))
                    updateStr += "'users': '" + userChoice[uc] + "',";
                if (!"false".equals(nodeChoice[nc]))
                    updateStr += "'nodes': '" + nodeChoice[nc] + "',";
                if (!"false".equals(startChoice[sc]))
                    updateStr += "'start': '" + startChoice[sc] + "',";
                updateStr += "}]";
                logstring = logItems.get(key).modifyReservation(updateStr);
                bgschedLog.write(logstring);
                choices = "startJob deleteRes".split(" ");
                nextStep = choices[rand.nextInt(choices.length)];
                logItems.put(nextStep + "_" + id[1], logItems.get(key));
                logItems.remove(key);
            }
            if(id[0].equals("deleteRes")) {
                logstring = logItems.get(key).deleteReservation();
                bgschedLog.write(logstring);
                logItems.remove(key);
            }
            if(id[0].equals("startJob")) {
                logstring = logItems.get(key).getJob().startJob();
                cqmLog.write(logstring);
                choices = "startJob modifyRes deleteRes".split(" ");
                nextStep = choices[rand.nextInt(choices.length)];
                logItems.put("terminateJob_" + id[1], logItems.get(key));
                logItems.remove(key);
            }
            if(id[0].equals("terminateJob")) {
                logstring = logItems.get(key).getJob().terminateJob(rand.nextInt(1));
                cqmLog.write(logstring);
                logItems.remove(key);
            }
        }

        cqmLog.close();
        bgschedLog.close();
    }

    public String[] getRandomSubArray(String[] array) {

        Random rand = new Random();
        int size = rand.nextInt(array.length);
        String[] selectedArray = new String[size];

        for(int i = 0; i < size; i++) {
            int index = rand.nextInt(array.length);
            selectedArray[i] = array[index];
        }

        return selectedArray;
    }

    private class Reservation {

        String name_;
        String[] users_;
        String[] nodes_;
        String startTime_;
        long duration_;
        Job job_;
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Reservation(String name, String[] users, String[] nodes, String startTime, long duration) {

            name_ = name;
            users_ = users;
            nodes_ = nodes;
            startTime_ = startTime;
            duration_ = duration;

            Date now = new Date();
            long jobStartTime = now.getTime();
            long jobEndTime = jobStartTime + duration;
            Random rand = new Random();
            job_ = new Job(rand.nextInt(1000000), name_, nodes_, users_, jobStartTime, jobEndTime, "/home");
        }

        String createReservation() {

            Date now = new Date();
            String date = sdfDate.format(now);
            String nodeStr = String.join(",", nodes_);
            String userStr = String.join(":", users_);

            String logstring = date + "user" + " adding reservation: [{'name': '" + name_ + "', 'partitions': '" +
                    nodeStr + "', 'start': '" + startTime_ + "', 'duration': '" + duration_ + "', 'users': '" +
                    userStr + "'}]";
            return logstring;
        }

        String modifyReservation(String updateStr) {

            Date now = new Date();
            String date = sdfDate.format(now);

            String logstring = date + " " + "testuser" + " modifying reservation: " + updateStr;
            return logstring;
        }

        String deleteReservation() {

            Date now = new Date();
            String date = sdfDate.format(now);

            String logstring = date + " " + "testuser" + " releasing reservation: [{'name': '" + name_ + "'}]";
            return logstring;
        }

        Job getJob() {
            return job_;
        }
    }

    private class Job {

        int jobid_;
        String name_;
        String[] nodes_;
        String[] users_;
        long startTime_;
        long endTime_;
        String workdir_;

        Job(int jobid, String name, String[] nodes, String[] users, long startTime, long endTime, String workdir){

            jobid_= jobid;
            name_ = name;
            nodes_ = nodes;
            users_ = users;
            startTime_ = startTime;
            endTime_ = endTime;
            workdir_ = workdir;
        }

        String startJob() {

            Date now = new Date();
            String date = sdfDate.format(now);
            String nodeStr = String.join(",", nodes_);
            String userStr = String.join(",", users_);
            int numNodes = nodes_.length;

            String logstring = date + " " + date + ";S;" + jobid_ + ";Resource_List.ncpus=" + numNodes +
                    " Resource_List.nodect=" + numNodes + " Resource_List.walltime=1:00:00 " +
                    "account=xxx args= ctime=1554251516.32 cwd=" + workdir_ + " etime=1554251516.32 " +
                    "exe=/home exec_host=\"" + nodeStr + "\" group=g jobname=" + name_ + " mode=script " +
                    "qtime=1554251516.32 queue=R.pm2 session=sss start=" + startTime_ + " user=" + userStr;
            return logstring;
        }

        String terminateJob(int exitStatus) {

            Date now = new Date();
            String date = sdfDate.format(now);
            String nodeStr = String.join(",", nodes_);
            String userStr = String.join(",", users_);
            int numNodes = nodes_.length;

            String logstring = date + " " + date + ";E;" + jobid_ + ";Exit_status==" + exitStatus +
                    " Resource_List.ncpus=" + numNodes +" Resource_List.nodect=" + numNodes +
                    " Resource_List.walltime=1:00:00 account=xxx approx_total_etime=20 args= ctime=1554251516.32 cwd=" +
                    workdir_ + " end=" + endTime_ + " etime=1554251516.32 exe=/home exec_host=" + nodeStr +
                    " group=g jobname=" + name_ + " mode=script priority_core_hours=2824063 qtime=1554251516.32" +
                    " queue=R.pm2 resources_used.location=" + nodeStr + " resources_used.nodect=" + numNodes +
                    " resources_used.walltime=0:03:23 session=sss start=" + startTime_ + " user=" + userStr;
            return logstring;
        }
    }
}
