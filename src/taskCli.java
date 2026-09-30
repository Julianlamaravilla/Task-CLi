import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class taskCli {

    // Those two lines define where and how your application references the file where tasks are saved on disk
    private static final String FILE_NAME = "task.json";
    private static final Path FILE_PATH = Paths.get(FILE_NAME);

    // Representing Task properties
    static class Task {
        int id;
        String description;
        String status; // todo , in-progress, done
        String createAt;
        String updatedAt;

        Task(int id, String description, String status, String createAt, String updatedAt){
            this.id = id;
            this.description = description;
            this.status = status;
            this.createAt = createAt;
            this.updatedAt = updatedAt;
        }
    }

    // Code checks whether the user passed any command-line arguments when running the program
    public static void main(String[] args){
        if(args.length == 0){
            printUsage();
            return;
        }

        String action = args[0].toLowerCase();
        List<Task> tasks = loadTasks();

        try {
            switch (action){
                case "add":
                    if(args.length < 2){
                        System.out.println("Error: Description required. Usage : task-cli add \"description\"");
                        return;
                    }
                    addTask(tasks, args[1]);
                    break;

                case "update":
                    if(args.length < 3){
                        System.out.println("Error : ID and description required. Usage: task-cli update <id> \"description\"");
                        return;
                    }
                    updateTask(tasks, Integer.parseInt(args[1]), args[2]);
                    break;

                case  "delete":
                    if(args.length < 2){
                        System.out.println("Error : ID required. Usage: tasks-cli delete <id>");
                        return;
                    }
                    deleteTask(tasks, Integer.parseInt(args[1]));
                    break;

                case "mark-in-progress":
                    if(args.length < 2){
                        System.out.print("Error: ID required. Usage: task-cli mark-in-progress <id>");
                        return;
                    }
                    updateStatus(tasks, Integer.parseInt(args[1]), "in-progress");
                    break;

                case "mark-done":
                    if (args.length < 2){
                        System.out.println("Error: ID required. Usage: tasks-cli mark-done <id>");
                        return;
                    }
                    updateStatus(tasks, Integer.parseInt(args[1]), "done");
                    break;

                case "list":
                    String filter = args.length > 1 ? args[1].toLowerCase() : "all";
                    listTasks(tasks, filter);
                    break;

                default:
                    System.out.println("Unknow command: " + action);
                    printUsage();
            }
        } catch (NumberFormatException e){
            System.out.println("Error: Task ID must be a valid number.");
        } catch (Exception e){
            System.out.println("An unexpected error occurred: " + e.getMessage() );
        }
    }




    // missing try and catch

    private static void addTask(List<Task> tasks , String description){
        int nextId = tasks.stream().mapToInt(t -> t.id).max().orElse(0) + 1;
        String now = getCurrentTimestamp();

        Task newTask = new Task(nextId , description, "todo", now , now);
        tasks.add(newTask);
        saveTasks(tasks);
    }

    private static void updateTask(List<Task> tasks , int id , String newDescription){
        Task task = findTasksById(tasks, id);
        if (task == null){
            System.out.println("Error: Task with ID" + id + "not found.");
            return;
        }

        task.description = newDescription;
        task.updatedAt = getCurrentTimestamp();
        saveTasks(tasks);

        System.out.print("Task " + id + " updated successfully");
    }

    private static void deleteTask(List<Task> tasks, int id){
        boolean removed = tasks.removeIf(t -> t.id == id);
        if(!removed){
            System.out.print("Error: Task with ID " + id + " not found");
            return;
        }

        saveTasks(tasks);
        System.out.print("Task " + id + " delete successfully");
    }

    private static void updateStatus(List<Task> tasks , int id, String status){
        Task task = findTasksById(tasks , id);
        if (task == null){
            System.out.println("Error : Task with ID  " + id + " not found");
        }

        task.status = status;
        task.updatedAt = getCurrentTimestamp();
        saveTasks(tasks);
    }

    private static void listTasks(List<Task> tasks , String filter){
        if (tasks.isEmpty()){
            System.out.println("No tasks found. ");
            return;
        }

        List<Task> filtered = new ArrayList<>();
        for( Task t : tasks){
            if("all".equals(filter) || t.status.equalsIgnoreCase(filter)){
                filtered.add(t);
            }
        }

        if (filtered.isEmpty()){
            System.out.print("No tasks found with status: " + filter);
            return;
        }

        System.out.printf("%-4s | %-12s | %-30s | %-20s%n", "ID", "Status", "Description", "Last Updated");
        System.out.println("----------------------------------------------------------------------");
        for (Task t : filtered) {
            System.out.printf("%-4d | %-12s | %-30s | %-20s%n", t.id, t.status, t.description, t.updatedAt);
        }
    }

    // missing  listTasks
    // --- lightweight Native JSON Persistence ---
    private static List<Task> loadTasks(){
        List<Task> tasks = new ArrayList<>();
        File file = FILE_PATH.toFile();

        if (!file.exists() || file.length() == 0){
            return tasks;
        }

        try{
            String jsonStr = new String(Files.readAllBytes(FILE_PATH)).trim();
            if (jsonStr.startsWith("[") && jsonStr.endsWith("]")){
                jsonStr = jsonStr.substring(1, jsonStr.length() - 1).trim();
                if (jsonStr.isEmpty()) return tasks;

                String[] objects = jsonStr.split("\\}\\s*,\\s*\\{");
                for (String objStr : objects){
                    objStr = objStr.replace("{","").replace("}","");
                    int id = Integer.parseInt(extractJsonValue(objStr, "id"));
                    String description = unescape(extractJsonValue(objStr, "description"));
                    String status = extractJsonValue(objStr, "status");
                    String createAt = extractJsonValue(objStr, "createAt");
                    String updateAt = extractJsonValue(objStr, "updateAt");

                    tasks.add(new Task(id,description, status, createAt, updateAt));
                }
            }
        } catch (Exception e){
            System.out.println("warning: Failed to parse tasks.json. Starting with clean state");
        }
        return tasks;
    }

    private static void saveTasks(List<Task> tasks) {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            json.append("  {\n")
                    .append("    \"id\": ").append(t.id).append(",\n")
                    .append("    \"description\": \"").append(escape(t.description)).append("\",\n")
                    .append("    \"status\": \"").append(t.status).append("\",\n")
                    .append("    \"createdAt\": \"").append(t.createAt).append("\",\n")
                    .append("    \"updatedAt\": \"").append(t.updatedAt).append("\"\n")
                    .append("  }").append(i < tasks.size() - 1 ? ",\n" : "\n");
        }
        json.append("]");

        try {
            Files.write(FILE_PATH, json.toString().getBytes());
        } catch (IOException e) {
            System.out.println("Error saving tasks to file: " + e.getMessage());
        }
    }

    // --- Helpers ---

    private static String extractJsonValue(String jsonObj, String key) {
        String keyPattern = "\"" + key + "\":";
        int startIndex = jsonObj.indexOf(keyPattern);
        if (startIndex == -1) return "";

        startIndex += keyPattern.length();
        int endIndex = jsonObj.indexOf(",", startIndex);
        if (endIndex == -1) endIndex = jsonObj.length();

        String val = jsonObj.substring(startIndex, endIndex).trim();
        if (val.startsWith("\"") && val.endsWith("\"")) {
            val = val.substring(1, val.length() - 1);
        }
        return val;
    }

    private static Task findTasksById(List<Task> tasks, int id){
        return tasks.stream().filter(t -> t.id == id).findFirst().orElse(null);
    }

    private static String getCurrentTimestamp(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String escape(String input){
        return input.replace("\"","\\\"");
    }

    private static String unescape(String input){
        return input.replace("\\\"", "\"");
    }

    private static void printUsage(){
        System.out.println("Usage:");
        System.out.println("  task-cli add \"<description>\"");
        System.out.println("  task-cli update <id> \"<description>\"");
        System.out.println("  task-cli delete <id>");
        System.out.println("  task-cli mark-in-progress <id>");
        System.out.println("  task-cli mark-done <id>");
        System.out.println("  task-cli list [todo|in-progress|done]");
    }
}