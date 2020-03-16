package com.intel.dai.eventsim;

import com.intel.logging.Logger;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class WlmApi {

    Logger logger_;
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String bgschedPath = "/var/log/bgsched.log";
    String cqmPath = "/var/log/cqm.log";

    public WlmApi(Logger logger) {
        logger_ = logger;
    }

    public String createReservation(String name, String users, String nodes, Timestamp starttime, String duration) throws IOException {

        logger_.info("Generate create reservation log line");
        String logstring = "";
        FileWriter bgschedLog = null;
        try {
            bgschedLog = new FileWriter(bgschedPath, StandardCharsets.UTF_8, true);
            String[] userArr = users.split(" ");
            String[] nodeArr = nodes.split(" ");

            Reservation reservation = new Reservation(name, userArr, nodeArr, starttime.getTime()/1000, Long.parseLong(duration), 0);
            logstring = reservation.createReservation();
            bgschedLog.write(logstring);
        }
        catch (IOException e) {
            logger_.exception(e, "Failed to open log file");
        }
        finally {
            if (bgschedLog != null)
                bgschedLog.close();
        }

        return logstring;
    }

    public String modifyReservation(String name, String users, String nodes, String starttime) throws IOException {

        logger_.info("Generate modify reservation log line");
        String logstring = "";
        FileWriter bgschedLog = null;
        try {
            bgschedLog = new FileWriter(bgschedPath, StandardCharsets.UTF_8, true);
            String updateStr = "[{";
            if (!"false".equals(users))
                updateStr += "'users': '" + users.replace(",",":").replace(" ",":") + "',";
            if (!"false".equals(nodes))
                updateStr += "'nodes': '" + nodes.replace(" ",",")  + "',";
            if (!"false".equals(starttime))
                updateStr += "'start': '" + Timestamp.valueOf(starttime).getTime()/1000 + "',";
            updateStr += "}]";

            Reservation reservation = new Reservation(name, null, null, 0, 0, 0);
            logstring = reservation.modifyReservation(updateStr);
            bgschedLog.write(logstring);
        }
        catch (IOException e) {
            logger_.exception(e, "Failed to open log file");
        }
        finally {
            if (bgschedLog != null)
                bgschedLog.close();
        }

        return logstring;
    }

    public String deleteReservation(String name) throws IOException {

        logger_.info("Generate delete reservation log line");
        FileWriter bgschedLog = null;
        String logstring = "";
        try {
            bgschedLog = new FileWriter(bgschedPath, StandardCharsets.UTF_8, true);
            Reservation reservation = new Reservation(name, null, null, 0, 0, 0);
            logstring = reservation.deleteReservation();
            bgschedLog.write(logstring);
        }
        catch (IOException e) {
            logger_.exception(e, "Failed to open log file");
        }
        finally {
            if (bgschedLog != null)
                bgschedLog.close();
        }

        return logstring;
    }


    public String startJob(String jobid, String name, String users, String nodes, Timestamp starttime, String workdir) throws IOException {

        logger_.info("Generate start job log line");
        FileWriter cqmLog = null;
        String logstring = "";
        try {
            cqmLog = new FileWriter(cqmPath, StandardCharsets.UTF_8, true);
            String[] userArr = users.split(" ");
            String[] nodeArr = nodes.split(" ");
            Job job_ = new Job(Integer.parseInt(jobid), name, nodeArr, userArr, starttime.getTime()/1000, 0, workdir);
            logstring = job_.startJob();
            cqmLog.write(logstring);
        }
        catch (IOException e) {
            logger_.exception(e, "Failed to open log file");
        }
        finally {
            if (cqmLog != null)
                cqmLog.close();
        }

        return logstring;
    }

    public String terminateJob(String jobid, String name, String users, String nodes, Timestamp starttime, String workdir, String exitStatus) throws IOException {

        logger_.info("Generate terminate job log line");
        FileWriter cqmLog = null;
        String logstring = "";
        try {
            cqmLog = new FileWriter(cqmPath, StandardCharsets.UTF_8, true);
            Date now = new Date();
            long endTime = now.getTime()/1000;
            String[] userArr = users.split(" ");
            String[] nodeArr = nodes.split(" ");
            Job job_ = new Job(Integer.parseInt(jobid), name, nodeArr, userArr, starttime.getTime()/1000, endTime, workdir);
            logstring = job_.terminateJob(Integer.parseInt(exitStatus));
            cqmLog.write(logstring);
        }
        catch (IOException e) {
            logger_.exception(e, "Failed to open log file");
        }
        finally {
            if (cqmLog != null)
                cqmLog.close();
        }

        return logstring;
    }

    public void simulateWlm(String reservations, String[] nodes) throws Exception {

        HashMap<String, Reservation> logItems = new HashMap<String, Reservation>();
        String[] users = "user1 user2 user3 user4".split(" ");
        Random rand = new Random();
        int numRes = Integer.parseInt(reservations);
        String[] reservedNodes, choices, id;
        Date now;
        String logstring, key, nextStep, updateStr;
        Reservation reservation;
        FileWriter cqmLog = null;
        FileWriter bgschedLog = null;

        try {
            cqmLog = new FileWriter(cqmPath, StandardCharsets.UTF_8, true);
            bgschedLog = new FileWriter(bgschedPath, StandardCharsets.UTF_8, true);

            for(int i = 0; i < numRes; i++) {
                reservedNodes = getRandomSubArray(nodes);
                now = new Date();
                reservation = new Reservation("reservation" + i, getRandomSubArray(users), reservedNodes, now.getTime()/1000, rand.nextInt(86400000), i);
                logItems.put("createRes_" + i, reservation);
            }

            while(!logItems.isEmpty()) {
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
                    int modify = rand.nextInt(7) + 1;
                    updateStr = "[{";
                    // Cases 1, 2, 3 and 4
                    if (modify < 5)
                        updateStr += "'users': '" + String.join(":", getRandomSubArray(users)) + "',";
                    // Cases 2, 4, 6, 7
                    if (modify % 2 == 0 || modify == 7)
                        updateStr += "'nodes': '" + String.join(",", getRandomSubArray(nodes)) + "',";
                    // Cases 3, 4, 5, 6
                    if (modify > 2 && modify < 7)
                        updateStr += "'start': " + (new Date()).getTime()/1000 + ".0,";
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
                    logItems.put("terminateJob_" + id[1], logItems.get(key));
                    logItems.remove(key);
                }
                if(id[0].equals("terminateJob")) {
                    logstring = logItems.get(key).getJob().terminateJob(rand.nextInt(2));
                    cqmLog.write(logstring);
                    logItems.remove(key);
                }
            }
        }
        catch (IOException e) {
            logger_.exception(e, "Failed to open log file");
        }
        finally {
            try {
                if (bgschedLog != null)
                    bgschedLog.close();
            }
            catch (IOException e) {
                logger_.exception(e, "Failed to close bgsched log file");
            }
            try {
                if (cqmLog != null)
                    cqmLog.close();
            }
            catch (IOException e) {
                logger_.exception(e, "Failed to close cqm log file");
            }
        }
    }

    public String[] getRandomSubArray(String[] array) {

        Random rand = new Random();
        int size = rand.nextInt(array.length);
        String[] selectedArray = new String[size];

        for(int i = 0; i < size; i++) {
            int index = rand.nextInt(array.length);
            selectedArray[i] = array[index];
        }

        //Remove duplicates
        selectedArray = new HashSet<String>(Arrays.asList(selectedArray)).toArray(new String[0]);

        return selectedArray;
    }

    private class Reservation {

        String name_;
        String[] users_;
        String[] nodes_;
        long startTime_;
        long duration_;
        Job job_;
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Reservation(String name, String[] users, String[] nodes, long startTime, long duration, int jobId) {

            name_ = name;
            users_ = users;
            nodes_ = nodes;
            startTime_ = startTime;
            duration_ = duration;

            Date now = new Date();
            long jobStartTime = now.getTime()/1000;
            long jobEndTime = jobStartTime + duration;
            job_ = new Job(jobId, name_, nodes_, users_, jobStartTime, jobEndTime, "/home");
        }

        String createReservation() {

            Date now = new Date();
            String date = sdfDate.format(now);
            String nodeStr = String.join(",", nodes_);
            String userStr = String.join(":", users_);

            String logstring = date + " testuser adding reservation: [{'name': '" + name_ + "', 'block_passthrough': False, 'partitions': '" +
                    nodeStr + "', 'project': dai, 'start': " + startTime_ + ".0, 'duration': " + duration_ + ", 'cycle': first, 'users': '" +
                    userStr + "'}]"  + "\n";
            return logstring;
        }

        String modifyReservation(String updateStr) {

            Date now = new Date();
            String date = sdfDate.format(now);

            String logstring = date + " testuser modifying reservation: [{'name': '" + name_ + "'}] with updates " + updateStr  + "\n";
            return logstring;
        }

        String deleteReservation() {

            Date now = new Date();
            String date = sdfDate.format(now);

            String logstring = date + " testuser releasing reservation: [{'name': '" + name_ + "'}]"  + "\n";
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
                    "qtime=1554251516.32 queue=R.pm2 session=sss start=" + startTime_ + " user=" + userStr + "\n";
            return logstring;
        }

        String terminateJob(int exitStatus) {

            Date now = new Date();
            String date = sdfDate.format(now);
            String nodeStr = String.join(",", nodes_);
            String userStr = String.join(",", users_);
            int numNodes = nodes_.length;

            String logstring = date + " " + date + ";E;" + jobid_ + ";Exit_status=" + exitStatus +
                    " Resource_List.ncpus=" + numNodes +" Resource_List.nodect=" + numNodes +
                    " Resource_List.walltime=1:00:00 account=xxx approx_total_etime=20 args= ctime=1554251516.32 cwd=" +
                    workdir_ + " end=" + endTime_ + " etime=1554251516.32 exe=/home exec_host=" + nodeStr +
                    " group=g jobname=" + name_ + " mode=script priority_core_hours=2824063 qtime=1554251516.32" +
                    " queue=R.pm2 resources_used.location=" + nodeStr + " resources_used.nodect=" + numNodes +
                    " resources_used.walltime=0:03:23 session=sss start=" + startTime_ + " user=" + userStr + "\n";
            return logstring;
        }
    }
}
