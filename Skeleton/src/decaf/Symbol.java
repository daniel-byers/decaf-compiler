package decaf;

// COPIED FROM ANTLR WEBSITE.

public class Symbol { // A generic programming language symbol
  public static enum Type {INVALID, VOID, INT, BOOLEAN}

  String name;      // All symbols at least have a name
  Type type;
  Scope scope;      // All symbols know what scope contains them.

  public Symbol(String name) { this.name = name; }
  public Symbol(String name, Type type) { this(name); this.type = type; }
  public String getName() { return name; }

  public String toString() {
    if (type != Type.INVALID) return getName() + ":" + type;
    return getName();
  }

  public static Symbol.Type getType(int tokenType) {
    switch (tokenType) {
      case DecafParser.VOID    : return Type.VOID;
      case DecafParser.INT     : return Type.INT;
      case DecafParser.BOOLEAN : return Type.BOOLEAN;
    }
    return Type.INVALID;
  }
}