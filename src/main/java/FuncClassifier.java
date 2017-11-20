import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.ast.Node;
import static equus.matching.PatternMatchers.*;

import java.util.*;

public class FuncClassifier extends JavaParserVisitorAdapter {
  public  HashMap<String, EnumSet<Flag>> funcs = new HashMap<>();
  private HashMap<String, EnumSet<Flag>> funcsNext = new HashMap<>();

  public FuncClassifier(
      List<String> normalizeFuncs,
      List<String> sanitizeFuncs,
      List<String> ioFuncs ) {
    super();
    normalizeFuncs.forEach(
        f -> funcs.put(f, EnumSet.of(Flag.PRIMITIVE, Flag.NORMALIZED)));
    sanitizeFuncs.forEach(
        f -> funcs.put(f, EnumSet.of(Flag.PRIMITIVE, Flag.SANITIZED)));
    ioFuncs.forEach(
        f -> funcs.put(f, EnumSet.of(Flag.PRIMITIVE, Flag.IO)));
    funcs.forEach((k,v) ->
        System.out.format("%s : %s%n", k, v.toString()));
  }

  // Finalize function attribute to step forward
  public void CompleteStep() {
    funcs     = funcsNext;
    funcsNext = new HashMap<>();
    // Deep copy
    funcs.forEach((k,v) -> funcsNext.put(k,v));
  }

  @Override
  public Object visit(JavaNode node, Object parentFuncName) {
    // Detect functions
    String name = match(node,
        // For declare detection
        caseType_(ASTConstructorDeclaration.class,
          n -> n.getQualifiedName().getOperation().split("\\(")[0]),
        caseType_(ASTMethodDeclaration.class,
          n -> n.getName()),

        // For call detection
        caseType_(ASTAllocationExpression.class,
          n -> getCName(n, "?", null)),
        caseType_(ASTPrimaryExpression.class,
          n -> getNameOfFuncCall(n, "?", null)),
        caseType_(ASTPrimarySuffix.class,
          n -> n.getImage()),

        //caseType_(ASTName.class, n -> getFuncName(n, "?")),
        // Ignore other nodes
        caseDefault_(o -> null) );

    boolean isFuncDecl = match(node,
        caseType_(ASTConstructorDeclaration.class,  n -> true),
        caseType_(ASTMethodDeclaration.class,       n -> true),
        caseDefault_(                               o -> false) );

    // Print detected func decl and call
    if(name!=null) {
      if(!isFuncDecl) System.out.print("  ");
      System.out.printf("%s %s <%d:%d>%n",
          node,
          name,
          node.getBeginLine(),
          node.getBeginColumn());
    }

    // Kernel: Update flags of parent func declaration if func call
    if(!isFuncDecl && name!=null) {
      assert parentFuncName != null;
      // Register and add flag
      // baka
      EnumSet<Flag> trgFlags =
        funcsNext.getOrDefault(parentFuncName, EnumSet.noneOf(Flag.class));

      // Modify trgFlags
      EnumSet<Flag> srcFlags =
        EnumSet.copyOf(funcs.getOrDefault(name, EnumSet.noneOf(Flag.class)));
      if(!srcFlags.contains(Flag.PRIMITIVE)) {
        srcFlags.remove(Flag.IO); // Ignore IO if not primitive
      }
      srcFlags.remove(Flag.PRIMITIVE);
      trgFlags.addAll(srcFlags);

      // Update
      funcsNext.put((String)parentFuncName, trgFlags);
    }

    return super.visit(node, isFuncDecl ? name : parentFuncName);
  }

  // Extract function name from image
  private <T extends JavaNode> String getFuncName(T n, String defaultName) {
    return Optional.ofNullable(n)
      .map(on -> on.getImage())
      .map(on -> on.split("\\.", 0))
      .map(on -> on[on.length-1])
      .orElse(defaultName);
  }

  // Extract constructor name if it is
  private <T extends ASTAllocationExpression> String getCName(
      T n, String defaultName, String notaConstructor) {
    ASTClassOrInterfaceType cit = n.getFirstChildOfType(ASTClassOrInterfaceType.class);
    ASTArguments a = n.getFirstChildOfType(ASTArguments.class);
    if(cit==null || a==null) return notaConstructor;

    // Now cit seems to be a constructor call
    return getFuncName(cit, defaultName);
  }

  // Extract function name if it is funccall
  private <T extends ASTPrimaryExpression> String getNameOfFuncCall(
      T n, String defaultName, String notaFunction) {
    ASTPrimaryPrefix pp = n.getFirstChildOfType(ASTPrimaryPrefix.class);
    ASTPrimarySuffix ps = n.getFirstChildOfType(ASTPrimarySuffix.class);
    if(pp==null || ps==null) return notaFunction;
    ASTArguments a = ps.getFirstChildOfType(ASTArguments.class);
    if(a==null) return notaFunction;

    // Now pp seems to be a function call
    return getFuncName(pp.getFirstChildOfType(ASTName.class), defaultName);
  }


}
