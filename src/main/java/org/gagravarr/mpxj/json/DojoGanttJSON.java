/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gagravarr.mpxj.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.TaskContainer;

/**
 * Converter which uses MPXJ to turn Microsoft Project files
 *  into JSON, in the format used by the Dojo Gantt library 
 */
@SuppressWarnings("unchecked")
public class DojoGanttJSON {
    public static String toJSON(ProjectFile project) throws IOException {
        StringWriter writer = new StringWriter();
        toJSON(project, writer);
        return writer.toString();
    }
    
    public static void toJSON(ProjectFile project, Writer out) throws IOException {
        JSONObject json = new JSONObject();
        
        // Boiler plate
        json.put("identifier", ID);        
        
        // Start from the root
        JSONArray items = new JSONArray();
        handleTaskContainer(project, items);
        json.put("items", items);
        
        // Have it output as JSON
        json.writeJSONString(out);
    }
    
    protected static final String ID = "id";
    protected static void handleTaskContainer(TaskContainer tasks, JSONArray items) {
        // The top level project needs slightly different keys to all others
        String childrenKey = "children";
        String startKey = "starttime";
        if (tasks instanceof ProjectFile) {
            childrenKey = "tasks";
            startKey = "startdate";
        }
        
        // Process the parent task, and recurse into children
        Task prevTask = null;
        for (Task task : tasks.getChildTasks()) {
            JSONObject json = new JSONObject();
            json.put(ID, task.getUniqueID());
            json.put("name", task.getName());
            
            if (prevTask == null) {
                // 1st children don't ever reference the parent
            } else {
                // If this is a second or subsequent child, and only if it
                //  itself doesn't have children, and only if this tasks starts
                //  after the previous one, output the previous Tasks's ID
                if (startsAfter(task, prevTask)) {
                    if (task.getChildTasks().size() == 0) {
                        json.put("previousTaskId", prevTask.getUniqueID());
                    }
                }
            }
            prevTask = task;
            
            json.put(startKey, formatDate(task.getStart()));
            // TODO Is the duration really in days, or something else?
            json.put("duration", formatInterval(task.getFinish(), task.getStart()));
            json.put("percentage", task.getPercentageComplete());
            
            // TODO The supply the remainder of the interesting details
            
            // Handle the resources, as best as we can
            StringBuffer taskOwner = new StringBuffer();
            for (ResourceAssignment ra : task.getResourceAssignments()) {
                Resource resource = ra.getResource();
                if (resource != null) {
                    if (taskOwner.length() > 0) {
                        taskOwner.append(", ");
                    }
                    taskOwner.append(resource.getName());
                }
            }
            json.put("taskOwner", taskOwner.toString());
            
            // Recurse into any children
            JSONArray children = new JSONArray();
            handleTaskContainer(task, children);
            json.put(childrenKey, children);
            
            items.add(json);
        }
    }
    
    /**
     * Checks to see if a given task starts after another one,
     * normally used when checking for overlaping vs sequential siblings
     */
    public static boolean startsAfter(Task task, Task prevTask) {
        if (task.getStart() != null && prevTask.getStart() != null) {
            // Check how the dates compare
            if (task.getStart().after(prevTask.getStart())) {
                return true;
            }
        }
        return false;
    }
    
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    /**
     * Returns a ISO 8601 representation of the given date. This method 
     * is thread safe and non-blocking.
     */
    public static String formatDate(Date date) {
        Calendar calendar = GregorianCalendar.getInstance(UTC, Locale.US);
        calendar.setTime(date);
        return String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }
    /**
     * Returns a ISO 8601 representation of the given date and time. This method 
     * is thread safe and non-blocking.
     */
    public static String formatDateTime(Date date) {
        Calendar calendar = GregorianCalendar.getInstance(UTC, Locale.US);
        calendar.setTime(date);
        return String.format(
                "%04d-%02d-%02dT%02d:%02d:%02dZ",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }
    
    public static final long DAY_IN_MS = 24*60*60*1000;
    public static final int DOJO_HOURS_IN_A_DAY = 8;
    private static final long MS_TO_HOURS = DAY_IN_MS / DOJO_HOURS_IN_A_DAY;
    public static int formatInterval(Date finish, Date start) {
        if (finish == null || start == null) {
            return 0;
        }
        long intervalMS = finish.getTime() - start.getTime();
        long intervalDojoHours = intervalMS / MS_TO_HOURS;
        return (int)intervalDojoHours;
    }
}
