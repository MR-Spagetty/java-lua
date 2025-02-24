package com.spag.lua;

public interface Util {
  static LuaString ls(String s){
    return LuaString.of(s);
  }
  static LuaBool lb(boolean b){
    return LuaBool.of(b);
  }
  static LuaBool lb(String b){
    return LuaBool.of(b);
  }
  static LuaNum ln(String n){
    return LuaNum.of(n);
  }
  static LuaNum ln(Object n){
    return LuaNum.of(n);
  }
}
