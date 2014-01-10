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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.management.RuntimeErrorException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.TaskContainer;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXReader;

/**
 * Command line wrapper around {@link DojoGanttJSON}
 */
public class ProjectToJSON {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Use:");
            System.err.println("   ProjectToJSON <project file> [json file]");
            System.exit(1);
        }
        
        ProjectFile project = null;
        File input = new File(args[1]);
        try {
            if (args[1].endsWith(".mpx")) {
                MPXReader reader = new MPXReader();
                project = reader.read(input);
            } else {
                MPPReader reader = new MPPReader();
                project = reader.read(input);
            }
        } catch (MPXJException e) {
            throw new RuntimeException("Error reading project file", e);
        }
        
        System.out.println(DojoGanttJSON.toJSON(project));
    }
}
