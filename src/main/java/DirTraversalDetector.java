import net.sourceforge.pmd.lang.ParserOptions;
import net.sourceforge.pmd.lang.java.Java18Parser;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DirTraversalDetector {
  public static void Run(String[] targets) throws IOException {
    // Sanitize targets
    for (String target : targets) {
      Path path = Paths.get(target);
      if(!Files.exists(path)) {
        System.out.format("File %s does not exists", path);
        return;
      }
      if(!path.toString().toLowerCase().endsWith(".java")) {
        System.out.format("File %s is not java", path);
        return;
      }
    }

    // Add primitive function attribute
    List<List<String>> lists = Arrays.asList(
      Files.readAllLines(Paths.get("rsrc/funclist_normalize")),
      Files.readAllLines(Paths.get("rsrc/funclist_sanitize")),
      Files.readAllLines(Paths.get("rsrc/funclist_io")) );
    FuncClassifier classifier =
      new FuncClassifier(lists.get(0), lists.get(1), lists.get(2));

    // Classify function recursively
    for (int i=0; i<3 ;i++) {
      // For each targets
      for (String target : targets) {
        Path path = Paths.get(target);
        try (BufferedReader br = Files.newBufferedReader(path)) {
          Java18Parser java18Parser = new Java18Parser(new ParserOptions());
          ASTCompilationUnit unit =
            (ASTCompilationUnit) java18Parser.parse(path.toString(), br);
          classifier.visit(unit, null);
        }
      }
      classifier.Complete();
    }

    // Check vulnerability
    EnumSet<Flag> reqFlags = EnumSet.of(Flag.NORMALIZED, Flag.SANITIZED);
    boolean safe = classifier.funcs.entrySet().stream().allMatch(e -> {
      //baka
      String k = e.getKey();
      EnumSet<Flag> v = e.getValue();

      if(v.contains(Flag.PRIMITIVE)) return true;

      // If this function uses io but (not normalized or not sanitized), warn
      System.out.format("%30s : %30s ", k, v.toString());
      if(v.contains(Flag.IO) && !v.containsAll(reqFlags)) {
        System.out.format("!!! VULNERABLE !!!%n");
        return false;
      } else {
        System.out.format("SAFE%n");
        return true;
      }
    });

    if(!safe) System.out.format("There are vulnerable functions.%n");

  }
}
