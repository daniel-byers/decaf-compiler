package decaf;

public class ArrayVariableSymbol extends VariableSymbol {
  int size;
  
  public ArrayVariableSymbol(String name, Type type, int size) { 
    super(name, type);
    this.size = size;
  }
}