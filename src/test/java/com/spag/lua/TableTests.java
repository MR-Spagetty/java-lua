package com.spag.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TableTests {
  @Test
  void basicAdd() {
    LuaTable table = new LuaTable();
    var s = LuaString.of("test");
    table.insert(s);
    var b = LuaBool.of("true");
    table.insert(b);
    var b2 = LuaBool.of("false");
    table.insert(b2);
    var i = LuaNum.of("123");
    table.insert(i);
    var f = LuaNum.of("1.23");
    table.insert(f);
    var zero = LuaNum.of("0");
    table.insert(zero);
    var zeroFloat = LuaNum.of("0.123");
    table.insert(zeroFloat);
    table.insert(LuaObject.nil);

    assertEquals(s, table.get(LuaNum.of(1)));
    assertEquals(b, table.get(LuaNum.of(2)));
    assertEquals(b2, table.get(LuaNum.of(3)));
    assertEquals(i, table.get(LuaNum.of(4)));
    assertEquals(f, table.get(LuaNum.of(5)));
    assertEquals(zero, table.get(LuaNum.of(6)));
    assertEquals(zeroFloat, table.get(LuaNum.of(7)));
    assertEquals(LuaObject.nil, table.get(LuaNum.of(8)));
  }

  @Test
  void put() {

    LuaTable table = new LuaTable();
    var s = LuaString.of("test");
    table.put(LuaString.of("String"), s);
    var b = LuaBool.of("true");
    table.put(LuaString.of("T"), b);
    var b2 = LuaBool.of("false");
    table.put(LuaString.of("F"), b2);
    var i = LuaNum.of("123");
    table.put(LuaString.of("int"), i);
    var f = LuaNum.of("1.23");
    table.put(LuaString.of("float"), f);
    var zero = LuaNum.of("0");
    table.put(LuaString.of("zero"), zero);
    var zeroFloat = LuaNum.of("0.123");
    table.put(LuaString.of("zero float"), zeroFloat);
    table.put(LuaString.of("nil"), LuaObject.nil);

    assertEquals(s, table.get("String"));
    assertEquals(b, table.get("T"));
    assertEquals(b2, table.get("F"));
    assertEquals(i, table.get("int"));
    assertEquals(f, table.get("float"));
    assertEquals(zero, table.get("zero"));
    assertEquals(zeroFloat, table.get("zero float"));
    assertEquals(LuaObject.nil, table.get("nil"));
    assertEquals(LuaObject.nil, table.get("not found"));
  }

  @Test
  void emptyTableToString() {
    LuaTable table = new LuaTable();
    assertEquals("{}", table.toString());
  }

  @Test
  void nilOnlyTableToString() {
    LuaTable table = new LuaTable();
    table.put("whatever", LuaObject.nil);
    assertEquals("{}", table.toString());
  }

  @Test
  void stringEqual() {
    String date = "03/02/70 04:28:08";
    String id = "1eb0a1e1-9a12-41e9-a297-76bd6485d70d";
    String start =
        "{\""
            + date
            + "\","
            + "id=\""
            + id
            + "\","
            + "data={\"init\",hasDHD=false,dialed=\"[]\",status=\"idle\",name=\"Chulak\"},"
            +"two={threee={4}}}";

    LuaTable intermed = LuaTable.fromString(start);
    assertEquals(LuaString.of(date), intermed.get(1));
    assertEquals(LuaString.of(id), intermed.get("id"));
    LuaTable data = (LuaTable) intermed.get("data");
    assertEquals(LuaString.of("init"), data.get(1));
    assertEquals(LuaBool.False, data.get("hasDHD"));
    assertEquals(LuaString.of("[]"), data.get("dialed"));
    assertEquals(LuaString.of("idle"), data.get("status"));
    assertEquals(LuaString.of("Chulak"), data.get("name"));
    assertEquals(start, intermed.toString());
  }

  @Test
  void nilToEmpty() {
    String init = "{wow=nil,a=nil,nil=nil}";
    assertEquals("{}", LuaTable.fromString(init).toString());
  }

  @Test
  void indexOnlyTableToString() {
    LuaTable table = new LuaTable();
    table.add(LuaString.of("test"));
    table.add(LuaBool.of("true"));
    table.add(LuaBool.of("false"));
    table.add(LuaNum.of("123"));
    table.add(LuaNum.of("1.23"));
    table.add(LuaNum.of("0"));
    table.add(LuaNum.of("0.123"));
    table.add(LuaObject.nil);
    table.add(new LuaTable());
    assertEquals("{\"test\",true,false,123,1.23,0,0.123,nil,{}}", table.toString());
  }
}
