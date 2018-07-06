/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

/**
 * Expressions can be grouped into categories by what methods exist under each context object. This
 * logic is quite ugly so it's been abstracted away into it's own class making it reusable.
 */
public class ExpressionOperationRules {
  public static boolean arithmeticBinaryOperation(DecafParser.ExprContext ctx) {
    return arithmeticReturnsInteger(ctx) || arithmeticReturnsBoolean(ctx);
  }

  public static boolean booleanBinaryOperations(DecafParser.ExprContext ctx) {
    return  arithmeticReturnsBoolean(ctx)
        ||  equalityBinaryOperations(ctx)
        ||  conditionalBinaryOperation(ctx);
  }

  public static boolean arithmeticReturnsInteger(DecafParser.ExprContext ctx) {
    return  ctx.MULTIPLY()    != null
        ||  ctx.DIVISION()    != null
        ||  ctx.MODULO()      != null
        ||  ctx.ADDITION()    != null
        ||  ctx.MINUS()       != null;
  }

  public static boolean arithmeticReturnsBoolean(DecafParser.ExprContext ctx) {
    return  ctx.LESSTHAN()    != null
        ||  ctx.GREATERTHAN() != null
        ||  ctx.LSSTHNEQTO()  != null
        ||  ctx.GRTTHNEQTO()  != null;
  }

  public static boolean equalityBinaryOperations(DecafParser.ExprContext ctx) {
    return  ctx.EQUAL()     != null
        ||  ctx.NOTEQUAL()  != null;
  }

  public static boolean conditionalBinaryOperation(DecafParser.ExprContext ctx) {
    return  ctx.AND() != null
        ||  ctx.OR()  != null;
  }
}