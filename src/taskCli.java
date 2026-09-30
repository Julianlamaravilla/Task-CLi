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
    }

    String action = args[0].toLoewrCase();
    List<Task> tasks = loadTasks();


    // missing try and catch

    private static void addTask(List<Task> tasks , String description){
        int nextId = tasks.stream().mapToInt(t -> t.id).max().orElse(0) + 1;
        String now = getCurrentTimestamp();

        Task newTask = new Task(nextId , description, "todo", now , now);
        tasks.add(newTask);
        saveTasks(tasks);
    }

    private static void updateTask(List<Task> tasks , int id , String newDescription){
        Task task = findTaskbyId(tasks, id);
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
        Task task = findTaskById(tasks , id);
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
                    String createAt = extracJsonvalue(objStr, "createAt");
                    String updateAt = extracJsonValue(objStr, "updateAt");

                    tasks.add(new Task(id,description, status, createAt, updateAt));
                }
            }
        } catch (Exception e){
            System.out.println("warning: Failed to parse tasks.json. Starting with clean state");
        }
        return tasks;
    }
}