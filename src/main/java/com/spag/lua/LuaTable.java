package com.spag.lua;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class to represent lua tables and decoded them from a serialized lua table
 * string (as per the Minecraft OpenComputers mod serialization library), some
 * specifics may not yet be implimented
 *
 * <p>
 * <b>NOTE</b>: indexed values index from 1 to preserve behaviour from lua for
 * pairs, ipairs and because tables may be speciallly formulated to account for
 * this
 *
 * @author MR_Spagetty
 */
public class LuaTable implements LuaObject {
  // TODO correct regex to match fixed behaviour
  static final String braceRegex = "^\\{(.*)\\}$";
  public static final Pattern bracePat = Pattern.compile(braceRegex);
  static final String numRegex = "[1-9][0-9]*\\.[0-9]+" + "|0\\.[0-9]+" + "|0|[1-9][0-9]*";
  public static final Pattern numPat = Pattern.compile(numRegex);
  static final String stringRegex = "\\\"\\\"|\\\".*?[^\\\\]\\\"";
  public static final Pattern stringPat = Pattern.compile(stringRegex);
  public static final Pattern indexed = Pattern.compile("\\G\\s*(" + stringRegex + "|" + numPat
      + "|\\{((?:[^{}]++|\\{(?:[^{}]++|\\{[^{}]*\\})*\\})*)\\}|" + LuaObject.nil.toString()
      + "|true|false)\\s*(?:,|\\Z)\\s*");
  public static final Pattern keyed = Pattern.compile("\\G\\s*(?:(\\w+)|\\[\\\"(.+)\\\"])=");
  private Map<LuaObject, LuaObject> dataByKey = new LinkedHashMap<>();

  private List<LuaObject> dataByIndex = new LinkedList<>();

  /**
   * emulates the bahviour of lua's ipairs loops
   *
   * @param iterator what to do for each iteration
   */
  public void ipairs(BiConsumer<LuaObject, LuaObject> iterator) {
    IntStream
        .range(1,
            Math.min(this.dataByIndex.indexOf(LuaObject.nil) + 1, this.dataByIndex.size() + 1))
        .forEach(i -> iterator.accept(LuaNum.of("" + i), this.dataByIndex.get(i - 1)));
  }

  /**
   * emulatees the behaviour of lua's pairs loops
   *
   * @param iterator what to do for each iteration
   */
  public void pairs(BiConsumer<LuaObject, LuaObject> iterator) {
    IntStream.range(1, dataByIndex.size()).forEach(i -> {
      LuaOptional.ofNilable(dataByIndex.get(i))
          .ifPresent(v -> iterator.accept(LuaNum.of("" + i), v));
    });
    dataByKey.entrySet().forEach(e -> {
      LuaObject key = e.getKey();
      iterator.accept(key, e.getValue());
    });
  }

  /**
   * Returns a sequential stream with the indexed data of this table as its
   * source.
   *
   * @return a sequential {@code Stream} over the indexed elements in this table
   */
  public Stream<LuaObject> stream() {
    return this.dataByIndex.stream();
  }

  /**
   * Returns a possibly parallel Stream with the indexed data of this table as its
   * source
   *
   * @return a possibly parallel {@code Stream} over the indexed elements in this
   *         table
   */
  public Stream<LuaObject> parallelStream() {
    return this.dataByIndex.parallelStream();
  }

  /**
   * Get the element at the given key
   *
   * <p>
   * if there is no element at the given key nil will be returned as in lua
   *
   * @param key the key to get the item at
   * @return the element at the given key or nil if ther eis no element at the
   *         given key
   * @throws NullPointerException if the given key is null
   */
  @Deprecated(forRemoval = true, since = "v2.0")
  public LuaObject get(String key) {
    Objects.requireNonNull(key, "Key may not be null");
    return get(LuaString.of(key));
  }

  /**
   * Gets the element at te given key/index
   * <p>
   * if there is no element at the given key/index nil will be returned as in lua.
   * <p>
   * additionally as in lua numbers which are positive integers are treated as
   * indexes
   *
   * @param keyInd the key/index to get the value at
   * @return the element at the given key or nil if there is no element at the
   *         given key
   * @throws NullPointerException if hte given key/index is null
   */
  public LuaObject get(LuaObject keyInd) {
    Objects.requireNonNull(keyInd, "Key/Index may not be null");
    if (keyInd instanceof LuaNum ln && ln.isPositive() && ln.isInteger()) {
      return getByIndex(ln.value.intValue());
    }
    return dataByKey.getOrDefault(keyInd, LuaObject.nil);
  }

  /**
   * put a new element in the table at the given key
   *
   * @param key   the key to put the element at
   * @param value the element to put at the key
   * @return the old element at the key
   * @throws NullPointerException if either the key or the element given are null
   * @implNote if the new element is nil this simply removes the old element
   */
  @Deprecated(forRemoval = true, since = "v2.0")
  public LuaObject put(String key, LuaObject value) {
    Objects.requireNonNull(key, "Key may not be null");
    LuaObject oldValue = get(key);
    put(LuaString.of(key), value);
    return oldValue;
  }

  /**
   * put a new element in the table at the given key or index
   * <p>
   * as per lua an item being stored at a positive integer will be an indexed item
   *
   * @param keyInd the key/index to put the new element at
   * @param value  the value to put in the table
   * @throws NullPointerException if either the key or the element given are null
   * @implNote if the new element is nil and it would be a keyed value it will
   *           just remove the old value
   */
  public void put(LuaObject keyInd, LuaObject value) {
    Objects.requireNonNull(keyInd, "Key may not be null");
    Objects.requireNonNull(value, "new Element may not be null");
    if (keyInd == nil) {
      throw new IllegalArgumentException("Key may not be nil");
    }
    if (keyInd instanceof LuaNum ln && ln.isInteger() && ln.isPositive()) {
      int index = ln.value.intValue();
      while (index >= this.dataByIndex.size()) {
        this.dataByIndex.add(nil);
      }
      this.dataByIndex.set(index - 1, value);
      while (this.dataByIndex.getLast() == nil) {
        this.dataByIndex.removeLast();
      }
      return;
    }
    if (value == LuaObject.nil) {
      if (this.dataByKey.containsKey(keyInd)) {
        dataByKey.remove(keyInd);
      }
      return;
    }

    this.dataByKey.put(keyInd, value);
  }

  /**
   * inserts the given value at the end of the indexed values of this table
   * 
   * @param value the value to insert
   * @see #insert(LuaNum, LuaObject)
   */
  public void insert(LuaObject value) {
    insert(LuaNum.of(dataByIndex.size() + 1), value);
  }

  /**
   * inserts the new value at the given index shunting items at the given index
   * and higher up
   *
   * @param index
   * @param value
   * @implNote indexed from 1 as per lua
   * @throws IllegalArgumentException  if the given index is not valid (i.e. non
   *                                   positive integers)
   * @throws IndexOutOfBoundsException if the given index is outside of the range
   *                                   of valid indexes
   */
  public void insert(LuaNum index, LuaObject value) {
    Objects.requireNonNull(index);
    Objects.requireNonNull(value);
    if (!index.isInteger() || !index.isPositive()) {
      throw new IllegalArgumentException("Expected positive integer for index");
    }
    int i = index.value.intValue();
    if (i > this.dataByIndex.size() + 1) {
      throw new IndexOutOfBoundsException(i);
    }
    this.dataByIndex.add(i - 1, value);
    while (this.dataByIndex.getLast() == nil) {
      this.dataByIndex.removeLast();
    }
  }

  /**
   * gets the element at the specified index
   *
   * @param index the indexs of the element to get
   * @return the element at the specifeid index
   * @implNote reminder indexed values are indexed from 1
   * @implNote indexes outside of the range of values stored will return nil
   */
  @Deprecated(forRemoval = true, since = "v2.0")
  public LuaObject get(int index) {
    return getByIndex(index);
  }

  private LuaObject getByIndex(int index) {
    if (index < 1 || index > this.dataByIndex.size()) {
      return LuaObject.nil;
    }
    return this.dataByIndex.get(index - 1);
  }

  /**
   * adds an element ot the end of the indexed elements in the table
   *
   * @param value the new element to add
   * @throws NullPointerException if the new element is null
   */
  @Deprecated(forRemoval = true, since = "v2.0")
  public void add(LuaObject value) {
    Objects.requireNonNull(value, "new element may not be null");
    this.dataByIndex.add(value);
  }

  /**
   * inserts the new element at the given index
   *
   * <p>
   * shifts element at and after the given index to a higher index
   *
   * @param index the index to insert he element at
   * @param value the element to insert at the given index
   * @throws NullPointerException if the new element is null
   * @implNote reminder indexed values are indexed from 1
   */
  @Deprecated(forRemoval = true, since = "v2.0")
  public void add(int index, LuaObject value) {
    Objects.requireNonNull(value, "new element may not be null");
    this.dataByIndex.add(index - 1, value);
  }

  /**
   * replaces the element at the given index
   *
   * @param i      the index to replace
   * @param newElm the new element
   * @return the old element at that index
   * @throws NullPointerException if the new element is null
   * @implNote reminder indexed values are indexed from 1
   */
  @Deprecated(forRemoval = true, since = "v2.0")
  public LuaObject replace(int i, LuaObject newElm) {
    return replaceAtIndex(i, newElm);
  }

  private LuaObject replaceAtIndex(int i, LuaObject newElm) {
    Objects.requireNonNull(newElm, "the new element may not be null");
    return this.dataByIndex.set(i - 1, newElm);
  }

  /**
   * equivilent to lua's {@code #} operator
   *
   * @return the number of indexed elements
   */
  public int size() {
    return this.dataByIndex.size();
  }

  @Override
  public String toString() {
    if (this.dataByIndex.isEmpty() && this.dataByKey.isEmpty()) {
      return "{}";
    }
    StringBuilder out = new StringBuilder();
    out.append("{");
    this.dataByIndex.forEach(v -> out.append(v + ","));
    this.dataByKey.entrySet().forEach(e -> {
      LuaObject key = e.getKey();
      out.append("%s=%s,".formatted(switch (key) {
      case LuaString ls -> ls.value.contains(" ") ? "[" + ls + "]" : ls.value;
      default -> "[" + key + "]";
      }, e.getValue().toString()));
    });
    out.setCharAt(out.lastIndexOf(","), '}');
    return out.toString();
  }

  /**
   * merges anouther luaTable into this one
   *
   * <p>
   * indexed items from the other table will be added after items from this one
   *
   * <p>
   * keyed items from the other table will overide corresponding items in this one
   *
   * @param b the table to merge from
   * @return this table after the merge operation to allow multiple consecutive
   *         opperations in a single statement
   */
  public LuaTable merge(LuaTable b) {
    Stream.concat(stream(), b.stream()).forEach(this::add);
    Stream
        .concat(this.dataByKey.entrySet().parallelStream().filter(
            e -> !b.dataByKey.containsKey(e.getKey())), b.dataByKey.entrySet().parallelStream())
        .forEach(e -> put(e.getKey(), e.getValue()));
    return this;
  }

  /**
   * creates a new table combinding the data from the given data where a is merged
   * first and then b
   *
   * @param a the first table to merge the data from
   * @param b the second table to merge the data from
   * @return the table containing th emerged data
   * @see #merge(LuaTable) LuaTable.merge(LuaTable) for merge rules
   */
  public static LuaTable merge(LuaTable a, LuaTable b) {
    LuaTable out = new LuaTable();
    out.merge(a);
    out.merge(b);
    return out;
  }

  /**
   * parses a LuaTable that is serialized into a string
   *
   * <p>
   * deserializes the table from the Minecraft OpenComputers mod's serialization
   * format into a representation of the oringonal LuaTable using the classes from
   * this Library
   *
   * @param data the serialized string representation of the table
   * @return the parsed table
   * @throws IllegalArgumentException if the table is invalid
   * @implNote may not currently allow all possible serialized data
   */
  public static LuaTable fromString(String data) {
    Matcher match = bracePat.matcher(data);
    if (!match.matches()) {
      throw new IllegalArgumentException("Invalid Lua table: " + data);
    }
    LuaTable out = new LuaTable();
    data = match.group(1);
    Matcher indexedValues = indexed.matcher(data);
    Matcher keyedValues = keyed.matcher(data);
    int end = 0;
    while (indexedValues.find(end) || keyedValues.find(end)) {
      while (indexedValues.find(end)) {
        String val = indexedValues.group(1);
        end = indexedValues.end();
        out.add(parseObject(val));
      }
      while (keyedValues.find(end)) {
        String key = keyedValues.group(1);
        if (!indexedValues.find(keyedValues.end())) {
          throw new IllegalArgumentException("Invalid lua table, no value for key: " + key);
        }
        String val = indexedValues.group(1);
        end = indexedValues.end();
        out.put(key, parseObject(val));
      }
    }
    return out;
  }

  private static LuaObject parseObject(String data) {
    Objects.requireNonNull(data, "Lua data may not be null");
    if (data.startsWith("{")) {
      return fromString(data);
    } else if (stringPat.matcher(data).matches()) {
      return LuaString.of(data.substring(1, data.length() - 1));
    } else if (numPat.matcher(data).matches()) {
      return LuaNum.of(data);
    } else if (data.equals("true") || data.equals("false")) {
      return LuaBool.of(data);
    } else if (data.equals(LuaObject.nil.toString())) {
      return LuaObject.nil;
    }
    throw new IllegalArgumentException("Unrecognised type detected: " + data);
  }

  @Override
  public String type() {
    return "LuaTable";
  }
}
