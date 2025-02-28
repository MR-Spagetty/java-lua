package com.spag.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import static com.spag.lua.Util.*;

public class TableTests {
  @Test
  void basicInsert() {
    LuaTable table = new LuaTable();
    var s = ls("test");
    table.insert(s);
    var b = lb("true");
    table.insert(b);
    var b2 = lb("false");
    table.insert(b2);
    var i = ln("123");
    table.insert(i);
    var f = ln("1.23");
    table.insert(f);
    var zero = ln("0");
    table.insert(zero);
    var zeroFloat = ln("0.123");
    table.insert(zeroFloat);
    table.insert(LuaObject.nil);

    assertEquals(s, table.get(ln(1)));
    assertEquals(b, table.get(ln(2)));
    assertEquals(b2, table.get(ln(3)));
    assertEquals(i, table.get(ln(4)));
    assertEquals(f, table.get(ln(5)));
    assertEquals(zero, table.get(ln(6)));
    assertEquals(zeroFloat, table.get(ln(7)));
    assertEquals(LuaObject.nil, table.get(ln(8)));
    assertEquals(LuaObject.nil, table.get(ln(20)));
  }

  @Test
  void put() {

    LuaTable table = new LuaTable();
    var s = ls("test");
    table.put(ls("String"), s);
    var b = lb("true");
    table.put(ls("T"), b);
    var b2 = lb("false");
    table.put(ls("F"), b2);
    var i = ln("123");
    table.put(ls("int"), i);
    var f = ln("1.23");
    table.put(ls("float"), f);
    var zero = ln("0");
    table.put(ls("zero"), zero);
    var zeroFloat = ln("0.123");
    table.put(ls("zero float"), zeroFloat);
    table.put(ls("nil"), LuaObject.nil);

    assertThrows(IllegalArgumentException.class, () -> table.put(LuaObject.nil, ls("nil key")));

    assertEquals(s, table.get(ls("String")));
    assertEquals(b, table.get(ls("T")));
    assertEquals(b2, table.get(ls("F")));
    assertEquals(i, table.get(ls("int")));
    assertEquals(f, table.get(ls("float")));
    assertEquals(zero, table.get(ls("zero")));
    assertEquals(zeroFloat, table.get(ls("zero float")));
    assertEquals(LuaObject.nil, table.get(ls("nil")));
    assertEquals(LuaObject.nil, table.get(ls("not found")));
  }

  @Test
  void emptyTableToString() {
    LuaTable table = new LuaTable();
    assertEquals("{}", table.toString());
  }

  @Test
  void nilOnlyTableToString() {
    LuaTable table = new LuaTable();
    table.put(ls("whatever"), LuaObject.nil);
    assertEquals("{}", table.toString());
  }

  @Test
  void stringEqual() {
    String date = "03/02/70 04:28:08";
    String id = "1eb0a1e1-9a12-41e9-a297-76bd6485d70d";
    String start = "{\"" + date + "\"," + "id=\"" + id + "\","
        + "data={\"init\",hasDHD=false,dialed=\"[]\",status=\"idle\",name=\"Chulak\"},"
        + "two={threee={4}}}";

    LuaTable intermed = LuaTable.fromString(start);
    assertEquals(ls(date), intermed.get(ln(1)));
    assertEquals(ls(id), intermed.get(ls("id")));
    LuaTable data = (LuaTable) intermed.get(ls("data"));
    assertEquals(ls("init"), data.get(ln(1)));
    assertEquals(LuaBool.False, data.get(ls("hasDHD")));
    assertEquals(ls("[]"), data.get(ls("dialed")));
    assertEquals(ls("idle"), data.get(ls("status")));
    assertEquals(ls("Chulak"), data.get(ls("name")));
    assertEquals(start, intermed.toString());
  }

  @Test
  void equal() {
    LuaTable a1 = mergeTableA();
    LuaTable a2 = mergeTableA();
    assertEquals(a1, a2);
    LuaTable b = mergeTableB();
    assertNotEquals(a2, b);
  }

  @Test
  void nilToEmpty() {
    String init = "{wow=nil,a=nil}";
    LuaTable res = LuaTable.fromString(init);
    assertEquals("{}", res.toString());
    assertEquals(new LuaTable(), res);
  }

  @Test
  void indexOnlyTableToString() {
    LuaTable table = new LuaTable();
    table.insert(ls("test"));
    table.insert(lb("true"));
    table.insert(lb("false"));
    table.insert(ln("123"));
    table.insert(ln("1.23"));
    table.insert(ln("0"));
    table.insert(ln("0.123"));
    table.insert(LuaObject.nil);
    table.insert(new LuaTable());
    assertEquals("{\"test\",true,false,123,1.23,0,0.123,{}}", table.toString());
    table.insert(ln(8), LuaObject.nil);
    assertEquals("{\"test\",true,false,123,1.23,0,0.123,nil,{}}", table.toString());
  }

  LuaTable mergeTableA() {
    return LuaTable.fromString("{a = 2, 1, 2, 3, b = 3}");
  }

  LuaTable mergeTableB() {
    return LuaTable.fromString("{b = 4, 4, c = 5}");
  }

  LuaTable mergeExpectedResult() {
    return LuaTable.fromString("{1,2,3,4,a=2,b=4,c=5}");
  }

  @Test
  void merge1() {
    LuaTable a = mergeTableA();
    LuaTable b = mergeTableB();
    assertEquals(mergeExpectedResult(), a.merge(b));
  }

  @Test
  void merge2() {
    LuaTable a = mergeTableA();
    LuaTable b = mergeTableB();
    assertEquals(mergeExpectedResult(), LuaTable.merge(a, b));
  }

  @Test
  void merge3() {
    LuaTable a = mergeTableA();
    LuaTable b = mergeTableB();
    assertEquals(mergeExpectedResult(), new LuaTable().merge(a).merge(b));
  }

  @Test
  void mergeTransativity() {
    LuaTable a = mergeTableA();
    LuaTable b = mergeTableB();
    LuaTable staticMerge = LuaTable.merge(a, b);
    LuaTable transitiveMerge = new LuaTable().merge(a).merge(b);
    a.merge(b);
    assertEquals(mergeExpectedResult(), a);
    assertEquals(a, staticMerge);
    assertEquals(a, transitiveMerge);
    assertEquals(staticMerge, transitiveMerge);
  }

  @Test
  void explicitStringKey() {
    LuaTable a = LuaTable.fromString("{[\"0test blah blah\"]=3}");
    LuaTable b = new LuaTable();
    b.put(ls("0test blah blah"), ln(3));
    assertEquals("{[\"0test blah blah\"]=3}", a.toString());
    assertEquals("{[\"0test blah blah\"]=3}", b.toString());
    assertEquals(a, b);
  }

  @Test
  void explicitStringSimplify() {
    LuaTable a = LuaTable.fromString("{[\"test\"]=0}");
    assertEquals("{test=0}", a.toString());
  }

  @Test
  void negIntKey() {
    LuaTable a = LuaTable.fromString("{[-123]=true}");
    LuaTable b = new LuaTable();
    b.put(ln(-123), lb(true));
    assertEquals(b, a);
    assertEquals("{[-123]=true}", a.toString());
  }

  @Test
  void negFloatKey() {
    LuaTable a = LuaTable.fromString("{[-1.23]=true}");
    LuaTable b = new LuaTable();
    b.put(ln(-1.23), lb(true));
    assertEquals(b, a);
    assertEquals("{[-1.23]=true}", a.toString());
  }

  @Test
  void zeroKey() {
    LuaTable a = LuaTable.fromString("{[0]=true}");
    LuaTable b = new LuaTable();
    b.put(ln(0), lb(true));
    assertEquals(b, a);
    assertEquals("{[0]=true}", a.toString());
  }
  @Test
  void posFloatKey() {
    LuaTable a = LuaTable.fromString("{[1.23]=true}");
    LuaTable b = new LuaTable();
    b.put(ln(1.23), lb(true));
    assertEquals(b, a);
    assertEquals("{[1.23]=true}", a.toString());
  }

  @Test 
  void posIntPut() {
    LuaTable a = new LuaTable();
    a.put(ln(5), lb(true));
    LuaTable b = LuaTable.fromString("{nil, nil, nil, nil, true}");
    assertEquals(b, a);
  }

  @Test
  void boolKey() {
    LuaTable a = LuaTable.fromString("{[true]=true}");
    LuaTable b = new LuaTable();
    b.put(lb(true), lb(true));
    assertEquals(b, a);
    assertEquals("{[true]=true}", a.toString());
  }

  @Test
  void boolImplicitStringKey() {
    assertThrows(IllegalArgumentException.class, () -> LuaTable.fromString("{true=0}"));
    assertThrows(IllegalArgumentException.class, () -> LuaTable.fromString("{false=0}"));
  }

  @Test
  void nilKey() {
    assertThrows(IllegalArgumentException.class, () -> LuaTable.fromString("{nil=0}"));
  }

  @Test
  void boolStringKeysToString() {
    String exp = "{[\"true\"]=1,[\"false\"]=0}";
    LuaTable a = LuaTable.fromString(exp);
    assertEquals(exp, a.toString());
  }
}
