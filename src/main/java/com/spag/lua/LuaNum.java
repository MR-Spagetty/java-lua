package com.spag.lua;

import static com.spag.lua.SoftFlyweightUtil.clearExpiredRefs;

import java.lang.ref.SoftReference;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * LuaNum is the representation of lua's number type which can represent both
 * integers and decimals like the LuaString type this attempts to maintain all
 * observable properties of the number type in lua
 *
 * @author MR_Spagetty
 */
public class LuaNum implements LuaObject, LuaConcatable, Comparable<LuaNum> {
  private static final Map<String, SoftReference<LuaNum>> cache = new HashMap<>();
  public final BigDecimal value;

  private LuaNum(BigDecimal value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value.toString();
  }

  /**
   * Gets the LuaNum representing the given value, should it not already exist a
   * new LuaNum with the given value will be created
   *
   * @param data the value to obtain the LuaNum of
   * @return the LuaNum
   * @throws NumberFormatException if the given value is not valid BigDecimal
   *                               representable numeric value
   */
  public static LuaNum of(String data) {
    clearExpiredRefs(cache);
    return cache.computeIfAbsent(data, val -> new SoftReference<>(new LuaNum(new BigDecimal(val))))
        .get();
  }

  /**
   * Converts the given value if applicable (numeric type) to a LuaNum
   *
   * @param data the value to convert
   * @return the converted LuaNum value
   * @throws IllegalArgumentException if the given value is not a numeric type
   */
  public static LuaNum of(Object data) {
    return switch (data) {
    case Long _ -> of(data.toString());
    case Integer _ -> of(data.toString());
    case Double _ -> of(data.toString());
    case Float _ -> of(data.toString());
    case BigDecimal _ -> of(data.toString());
    default -> throw new IllegalArgumentException(
        "Unrecognised type detected: " + data.getClass().getName());
    };
  }

  /**
   * check if this LuaNum represents an integer value
   * 
   * @return true if this represents an integer, false otherwise
   */
  public boolean isInteger() {
    return this.value.remainder(BigDecimal.ONE).equals(BigDecimal.ZERO);
  }

  /**
   * check if this LuaNum represents a positive value
   *
   * @return true if this LuaNum represents a positive value, false otherwise
   */
  public boolean isPositive() {
    return this.value.compareTo(BigDecimal.ZERO) > 0;
  }

  @Override
  public String type() {
    return "LuaNum";
  }

  @Override
  public int compareTo(LuaNum o) {
    return this.value.compareTo(o.value);
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }
}
