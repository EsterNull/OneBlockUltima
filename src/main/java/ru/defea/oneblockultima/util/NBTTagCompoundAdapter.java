package ru.defea.oneblockultima.util;

import com.google.gson.*;
import net.minecraft.nbt.*;

import java.lang.reflect.Type;
import java.util.Map;

public class NBTTagCompoundAdapter implements JsonSerializer<NBTTagCompound>, JsonDeserializer<NBTTagCompound> {

    @Override
    public JsonElement serialize(NBTTagCompound src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null || src.hasNoTags()) {
            return JsonNull.INSTANCE;
        }
        JsonObject obj = new JsonObject();
        for (Object keyObj : src.getKeySet()) {
            String key = keyObj.toString();
            NBTBase tag = src.getTag(key);
            obj.add(key, serializeTag(tag, context));
        }
        return obj;
    }

    private JsonElement serializeTag(NBTBase tag, JsonSerializationContext context) {
        if (tag instanceof NBTTagString) {
            return new JsonPrimitive(((NBTTagString) tag).getString());
        } else if (tag instanceof NBTTagInt) {
            return new JsonPrimitive(((NBTTagInt) tag).getInt());
        } else if (tag instanceof NBTTagByte) {
            return new JsonPrimitive(((NBTTagByte) tag).getByte());
        } else if (tag instanceof NBTTagShort) {
            return new JsonPrimitive(((NBTTagShort) tag).getShort());
        } else if (tag instanceof NBTTagLong) {
            return new JsonPrimitive(((NBTTagLong) tag).getLong());
        } else if (tag instanceof NBTTagFloat) {
            return new JsonPrimitive(((NBTTagFloat) tag).getFloat());
        } else if (tag instanceof NBTTagDouble) {
            return new JsonPrimitive(((NBTTagDouble) tag).getDouble());
        } else if (tag instanceof NBTTagCompound) {
            return serialize((NBTTagCompound) tag, NBTTagCompound.class, context);
        } else if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            JsonArray array = new JsonArray();
            NBTTagList copy = (NBTTagList) list.copy();
            while (copy.tagCount() > 0) {
                array.add(serializeTag(copy.removeTag(0), context));
            }
            return array;
        } else if (tag instanceof NBTTagIntArray) {
            NBTTagIntArray intArray = (NBTTagIntArray) tag;
            JsonArray array = new JsonArray();
            for (int value : intArray.getIntArray()) {
                array.add(new JsonPrimitive(value));
            }
            return array;
        } else if (tag instanceof NBTTagByteArray) {
            NBTTagByteArray byteArray = (NBTTagByteArray) tag;
            JsonArray array = new JsonArray();
            for (byte value : byteArray.getByteArray()) {
                array.add(new JsonPrimitive(value));
            }
            return array;
        }
        return JsonNull.INSTANCE;
    }

    @Override
    public NBTTagCompound deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return new NBTTagCompound();
        }
        NBTTagCompound result = new NBTTagCompound();
        JsonObject obj = json.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            NBTBase tag = deserializeTag(value);
            if (tag != null) {
                result.setTag(key, tag);
            }
        }
        return result;
    }

    private NBTBase deserializeTag(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return new NBTTagString(primitive.getAsString());
            } else if (primitive.isNumber()) {
                Number number = primitive.getAsNumber();
                // Determine the number type
                if (number instanceof Byte) {
                    return new NBTTagByte(number.byteValue());
                } else if (number instanceof Short) {
                    return new NBTTagShort(number.shortValue());
                } else if (number instanceof Integer) {
                    return new NBTTagInt(number.intValue());
                } else if (number instanceof Long) {
                    return new NBTTagLong(number.longValue());
                } else if (number instanceof Float) {
                    return new NBTTagFloat(number.floatValue());
                } else if (number instanceof Double) {
                    return new NBTTagDouble(number.doubleValue());
                }
                // Default to Integer
                return new NBTTagInt(primitive.getAsInt());
            } else if (primitive.isBoolean()) {
                return new NBTTagByte((byte) (primitive.getAsBoolean() ? 1 : 0));
            }
        } else if (element.isJsonObject()) {
            return deserialize(element, NBTTagCompound.class, null);
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() == 0) {
                return new NBTTagList();
            }

            // Check whether all elements are numbers of the same type
            JsonElement firstElement = array.get(0);

            if (firstElement.isJsonPrimitive() && firstElement.getAsJsonPrimitive().isNumber()) {
                boolean allNumbers = true;
                for (JsonElement elem : array) {
                    if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isNumber()) {
                        allNumbers = false;
                        break;
                    }
                }

                if (allNumbers) {
                    // Check whether all elements fit in a byte
                    boolean allBytes = true;
                    for (JsonElement elem : array) {
                        int val = elem.getAsInt();
                        if (val < Byte.MIN_VALUE || val > Byte.MAX_VALUE) {
                            allBytes = false;
                            break;
                        }
                    }

                    if (allBytes) {
                        byte[] bytes = new byte[array.size()];
                        for (int i = 0; i < array.size(); i++) {
                            bytes[i] = array.get(i).getAsByte();
                        }
                        return new NBTTagByteArray(bytes);
                    } else {
                        int[] ints = new int[array.size()];
                        for (int i = 0; i < array.size(); i++) {
                            ints[i] = array.get(i).getAsInt();
                        }
                        return new NBTTagIntArray(ints);
                    }
                }
            }

            // Otherwise create an NBTTagList with mixed-type elements
            NBTTagList list = new NBTTagList();
            for (JsonElement elem : array) {
                NBTBase tag = deserializeTag(elem);
                if (tag != null) {
                    list.appendTag(tag);
                }
            }
            return list;
        }
        return null;
    }
}