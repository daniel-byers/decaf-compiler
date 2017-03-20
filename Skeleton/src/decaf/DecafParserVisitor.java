// Generated from /home/dann/MEGAsync/workspace/java/decaf-daniel-byers/Skeleton/src/decaf/DecafParser.g4 by ANTLR 4.6
package decaf;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DecafParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DecafParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link DecafParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(DecafParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#fieldDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDecl(DecafParser.FieldDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#arrayDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayDecl(DecafParser.ArrayDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#methodDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDecl(DecafParser.MethodDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#methodName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodName(DecafParser.MethodNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(DecafParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#varDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDecl(DecafParser.VarDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(DecafParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(DecafParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#assignOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignOp(DecafParser.AssignOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#methodCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodCall(DecafParser.MethodCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#calloutArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCalloutArg(DecafParser.CalloutArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#location}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocation(DecafParser.LocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DecafParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(DecafParser.ExprContext ctx);
}