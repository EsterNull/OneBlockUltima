package ru.defea.oneblockultima.util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import com.google.gson.*;
import net.minecraft.nbt.*;

import java.lang.reflect.Type;
import java.util.Map;

public class CompoundTagAdapter implements JsonSerializer<CompoundTag>, JsonDeserializer<CompoundTag> {

    @Override
    public JsonElement serialize(CompoundTag src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null || src.isEmpty()) {
            return JsonNull.INSTANCE;
        }
        JsonObject obj = new JsonObject();
        for (String key : src.getAllKeys()) {
            Tag tag = src.get(key);
            obj.add(key, serializeTag(tag, context));
        }
        return obj;
    }

    private JsonElement serializeTag(Tag tag, JsonSerializationContext context) {
        if (tag instanceof StringTag) {
            return new JsonPrimitive(((StringTag) tag).getAsString());
        } else if (tag instanceof IntTag) {
            return new JsonPrimitive(((IntTag) tag).getAsInt());
        } else if (tag instanceof ByteTag) {
            return new JsonPrimitive(((ByteTag) tag).getAsByte());
        } else if (tag instanceof ShortTag) {
            return new JsonPrimitive(((ShortTag) tag).getAsShort());
        } else if (tag instanceof LongTag) {
            return new JsonPrimitive(((LongTag) tag).getAsLong());
        } else if (tag instanceof FloatTag) {
            return new JsonPrimitive(((FloatTag) tag).getAsFloat());
        } else if (tag instanceof DoubleTag) {
            return new JsonPrimitive(((DoubleTag) tag).getAsDouble());
        } else if (tag instanceof CompoundTag) {
            return serialize((CompoundTag) tag, CompoundTag.class, context);
        } else if (tag instanceof ListTag) {
            ListTag list = (ListTag) tag;
            JsonArray array = new JsonArray();
            for (int i = 0; i < list.size(); i++) {
                array.add(serializeTag(list.get(i), context));
            }
            return array;
        } else if (tag instanceof IntArrayTag) {
            int[] values = ((IntArrayTag) tag).getAsIntArray();
            JsonArray array = new JsonArray();
            for (int value : values) {
                array.add(new JsonPrimitive(value));
            }
            return array;
        } else if (tag instanceof ByteArrayTag) {
            byte[] values = ((ByteArrayTag) tag).getAsByteArray();
            JsonArray array = new JsonArray();
            for (byte value : values) {
                array.add(new JsonPrimitive(value));
            }
            return array;
        }
        return JsonNull.INSTANCE;
    }

    @Override
    public CompoundTag deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return new CompoundTag();
        }
        CompoundTag result = new CompoundTag();
        JsonObject obj = json.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            Tag tag = deserializeTag(value);
            if (tag != null) {
                result.put(key, tag);
            }
        }
        return result;
    }

    private Tag deserializeTag(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return StringTag.valueOf(primitive.getAsString());
            } else if (primitive.isNumber()) {
                Number number = primitive.getAsNumber();
                if (number instanceof Byte) {
                    return ByteTag.valueOf(number.byteValue());
                } else if (number instanceof Short) {
                    return ShortTag.valueOf(number.shortValue());
                } else if (number instanceof Integer) {
                    return IntTag.valueOf(number.intValue());
                } else if (number instanceof Long) {
                    return LongTag.valueOf(number.longValue());
                } else if (number instanceof Float) {
                    return FloatTag.valueOf(number.floatValue());
                } else if (number instanceof Double) {
                    return DoubleTag.valueOf(number.doubleValue());
                }
                return IntTag.valueOf(primitive.getAsInt());
            } else if (primitive.isBoolean()) {
                return ByteTag.valueOf((byte) (primitive.getAsBoolean() ? 1 : 0));
            }
        } else if (element.isJsonObject()) {
            return deserialize(element, CompoundTag.class, null);
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() == 0) {
                return new ListTag();
            }

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
                        return new ByteArrayTag(bytes);
                    } else {
                        int[] ints = new int[array.size()];
                        for (int i = 0; i < array.size(); i++) {
                            ints[i] = array.get(i).getAsInt();
                        }
                        return new IntArrayTag(ints);
                    }
                }
            }

            ListTag list = new ListTag();
            for (JsonElement elem : array) {
                Tag tag = deserializeTag(elem);
                if (tag != null) {
                    list.add(tag);
                }
            }
            return list;
        }
        return null;
    }
}
