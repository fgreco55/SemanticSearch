package filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLister {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java FileLister <start-directory> [extensions...]");
            System.exit(1);
        }

        String startingPath = args[0];
        File root = new File(startingPath);

        List<String> matchExtensions = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String ext = args[i].toLowerCase();
            if (!ext.startsWith(".")) {
                ext = "." + ext;
            }
            matchExtensions.add(ext);
        }

        if (root.exists() && root.isDirectory()) {
            listFilesRecursive(root, 0, matchExtensions);
        } else {
            System.err.println("Invalid directory: " + startingPath);
        }
    }

    private static void listFilesRecursive(File dir, int indentLevel, List<String> matchExtensions) {
        File[] entries = dir.listFiles();
        if (entries == null) return;

        for (File entry : entries) {
            boolean highlight = entry.isFile() && matchesExtension(entry.getName(), matchExtensions);
            printWithIndent(entry.getName(), indentLevel, highlight);

            if (entry.isDirectory()) {
                listFilesRecursive(entry, indentLevel + 1, matchExtensions);
            }
        }
    }

    private static boolean matchesExtension(String fileName, List<String> matchExtensions) {
        String lower = fileName.toLowerCase();
        for (String ext : matchExtensions) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static void printWithIndent(String name, int indent, boolean highlight) {
        for (int i = 0; i < indent; i++) {
            System.out.print("    "); // 4 spaces per level
        }
        //System.out.println(name + (highlight ? " *" : ""));
        System.out.println(name + (highlight ? " <---------------------" : ""));

    }
}
