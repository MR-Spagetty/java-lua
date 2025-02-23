package com.spag.lua;
import java.util.Optional;
public final class LuaOptional {
private LuaOptional() {}
  public static Optional<LuaObject> ofNilable(LuaObject value){
    if (value == LuaObject.nil){
      return Optional.empty();
    }
    return Optional.of(value);
  }
}
