import net.sourceforge.pmd.lang.ParserOptions;
import net.sourceforge.pmd.lang.java.Java18Parser;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DirTraversalDetector {
  public static void Run() throws IOException {
    //Path path = Paths.get("src/main/java/DirTraversalDetector.java");
    Path path = Paths.get("rsrc/HelloWorld.java");
    if(!Files.exists(path)) {
      System.out.println("file not exists");
      return;
    }

    try (BufferedReader br = Files.newBufferedReader(path)) {
      Java18Parser java18Parser = new Java18Parser(new ParserOptions());
      ASTCompilationUnit unit =
        (ASTCompilationUnit) java18Parser.parse(path.getFileName().toString(), br);
      // baka
      List<List<String>> lists = Arrays.asList(
        Files.readAllLines(Paths.get("rsrc/funclist_normalize")),
        Files.readAllLines(Paths.get("rsrc/funclist_sanitize")),
        Files.readAllLines(Paths.get("rsrc/funclist_io")) );
      FuncClassifier visitor =
        new FuncClassifier(lists.get(0), lists.get(1), lists.get(2));
      visitor.visit(unit, null);

      EnumSet<Flag> vulnflags = EnumSet.of(Flag.NORMALIZED, Flag.SANITIZED);
      visitor.funcsNext.forEach((k,v) -> {
        if(v.contains(Flag.PRIMITIVE)) return;
        String vuln =
          !v.contains(Flag.IO)      ? "" :
          !v.containsAll(vulnflags) ? "!!! VULNERABLE !!!" : "SAFE";
        System.out.format("%s : %s %s%n", k, v.toString(), vuln);
      });
    }
  }
}
