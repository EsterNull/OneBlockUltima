package ru.defea.oneblockultima.util;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Bridges 1.12.2-style namespaced mob ids ("minecraft:zombie") used in the shared
 * config with the plain 1.7.10 {@link EntityList#stringToClassMapping} keys ("Zombie").
 */
public final class MobIdUtil
{
    /** normalized 1.12.2-style id (no namespace, no underscores) -> 1.7.10 EntityList key */
    private static final Map<String, String> VANILLA_KEYS = new HashMap<>();

    /** 1.7.10 EntityList key -> 1.12.2-style id tail */
    private static final Map<String, String> VANILLA_IDS = new HashMap<>();

    static
    {
        VANILLA_KEYS.put("zombie", "Zombie");
        VANILLA_KEYS.put("skeleton", "Skeleton");
        VANILLA_KEYS.put("creeper", "Creeper");
        VANILLA_KEYS.put("spider", "Spider");
        VANILLA_KEYS.put("cavespider", "CaveSpider");
        VANILLA_KEYS.put("enderman", "Enderman");
        VANILLA_KEYS.put("witch", "Witch");
        VANILLA_KEYS.put("slime", "Slime");
        VANILLA_KEYS.put("magmacube", "LavaSlime");
        VANILLA_KEYS.put("magma_cube", "LavaSlime");
        VANILLA_KEYS.put("lavaslime", "LavaSlime");
        VANILLA_KEYS.put("blaze", "Blaze");
        VANILLA_KEYS.put("ghast", "Ghast");
        VANILLA_KEYS.put("snowgolem", "SnowMan");
        VANILLA_KEYS.put("snowman", "SnowMan");
        VANILLA_KEYS.put("ocelot", "Ozelot");
        VANILLA_KEYS.put("mooshroom", "MushroomCow");
        VANILLA_KEYS.put("mushroomcow", "MushroomCow");
        VANILLA_KEYS.put("pigzombie", "PigZombie");
        VANILLA_KEYS.put("zombie_pigman", "PigZombie");
        VANILLA_KEYS.put("zombiepigman", "PigZombie");
        VANILLA_KEYS.put("villagergolem", "VillagerGolem");
        VANILLA_KEYS.put("iron_golem", "VillagerGolem");
        VANILLA_KEYS.put("irongolem", "VillagerGolem");
        VANILLA_KEYS.put("squid", "Squid");
        VANILLA_KEYS.put("bat", "Bat");
        VANILLA_KEYS.put("sheep", "Sheep");
        VANILLA_KEYS.put("cow", "Cow");
        VANILLA_KEYS.put("pig", "Pig");
        VANILLA_KEYS.put("chicken", "Chicken");
        VANILLA_KEYS.put("wolf", "Wolf");
        VANILLA_KEYS.put("villager", "Villager");
        VANILLA_KEYS.put("skeletonhorse", "EntityHorse");
        VANILLA_KEYS.put("zombiehorse", "EntityHorse");
        VANILLA_KEYS.put("horse", "EntityHorse");
        VANILLA_KEYS.put("enderdragon", "EnderDragon");
        VANILLA_KEYS.put("endercrystal", "EnderCrystal");
        VANILLA_KEYS.put("zombievillager", "Zombie");

        VANILLA_IDS.put("Zombie", "zombie");
        VANILLA_IDS.put("Skeleton", "skeleton");
        VANILLA_IDS.put("Creeper", "creeper");
        VANILLA_IDS.put("Spider", "spider");
        VANILLA_IDS.put("CaveSpider", "cave_spider");
        VANILLA_IDS.put("Enderman", "enderman");
        VANILLA_IDS.put("Witch", "witch");
        VANILLA_IDS.put("Slime", "slime");
        VANILLA_IDS.put("LavaSlime", "magma_cube");
        VANILLA_IDS.put("Blaze", "blaze");
        VANILLA_IDS.put("Ghast", "ghast");
        VANILLA_IDS.put("SnowMan", "snowman");
        VANILLA_IDS.put("Ozelot", "ocelot");
        VANILLA_IDS.put("MushroomCow", "mooshroom");
        VANILLA_IDS.put("PigZombie", "zombie_pigman");
        VANILLA_IDS.put("VillagerGolem", "villager_golem");
        VANILLA_IDS.put("Squid", "squid");
        VANILLA_IDS.put("Bat", "bat");
        VANILLA_IDS.put("Sheep", "sheep");
        VANILLA_IDS.put("Cow", "cow");
        VANILLA_IDS.put("Pig", "pig");
        VANILLA_IDS.put("Chicken", "chicken");
        VANILLA_IDS.put("Wolf", "wolf");
        VANILLA_IDS.put("Villager", "villager");
        VANILLA_IDS.put("EntityHorse", "horse");
        VANILLA_IDS.put("EnderDragon", "ender_dragon");
        VANILLA_IDS.put("EnderCrystal", "ender_crystal");
        VANILLA_IDS.put("Silverfish", "silverfish");
        VANILLA_IDS.put("WitherBoss", "wither");
        VANILLA_IDS.put("WitherSkull", "wither_skull");
        VANILLA_IDS.put("Giant", "giant");
    }

    private MobIdUtil()
    {
    }

    public static Entity createEntity(String registry, World world)
    {
        if (registry == null || registry.isEmpty() || world == null)
        {
            return null;
        }
        String key = resolveKey(registry);
        if (key == null)
        {
            return null;
        }
        try
        {
            Entity entity = EntityList.createEntityByName(key, world);
            // The 1.7.10 EntitySlime constructor randomizes the size (1/2/4).
            // Normalize it so GUI icons always show the smallest slime.
            if (entity instanceof EntitySlime)
            {
                try
                {
                    // setSlimeSize(int) is protected in 1.7.10, so use reflection.
                    java.lang.reflect.Method setSlimeSize = findMethodByName(EntitySlime.class,
                            new Class<?>[] { int.class }, "setSlimeSize", "func_70799_a");
                    if (setSlimeSize != null)
                    {
                        setSlimeSize.invoke(entity, 1);
                    }
                }
                catch (Exception ignored)
                {
                }
            }
            return entity;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static java.lang.reflect.Method findMethodByName(Class<?> clazz, Class<?>[] paramTypes, String... names)
    {
        for (String name : names)
        {
            try
            {
                java.lang.reflect.Method method = clazz.getDeclaredMethod(name, paramTypes);
                method.setAccessible(true);
                return method;
            }
            catch (Exception ignored)
            {
            }
        }
        return null;
    }

    public static String resolveKey(String registry)
    {
        if (registry == null)
        {
            return null;
        }
        if (EntityList.stringToClassMapping.containsKey(registry))
        {
            return registry;
        }

        String name = registry;
        int colonIndex = registry.indexOf(':');
        if (colonIndex >= 0)
        {
            name = registry.substring(colonIndex + 1);
        }

        String normalized = name.toLowerCase(Locale.ROOT).replace("_", "");

        String alias = VANILLA_KEYS.get(normalized);
        if (alias != null && EntityList.stringToClassMapping.containsKey(alias))
        {
            return alias;
        }

        for (Object keyObj : EntityList.stringToClassMapping.keySet())
        {
            String key = keyObj.toString();
            String normalizedKey = key.toLowerCase(Locale.ROOT).replace("_", "");
            if (normalizedKey.equals(normalized))
            {
                return key;
            }
        }

        String camel = toCamelCase(name);
        if (EntityList.stringToClassMapping.containsKey(camel))
        {
            return camel;
        }

        String searchName = name;
        if (colonIndex >= 0)
        {
            String namespace = registry.substring(0, colonIndex);
            if (searchName.startsWith(namespace + "."))
            {
                searchName = searchName.substring(namespace.length() + 1);
            }
        }
        String searchKey = searchName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (searchKey.length() >= 5)
        {
            for (Object keyObj : EntityList.stringToClassMapping.keySet())
            {
                Object classObj = EntityList.stringToClassMapping.get(keyObj);
                if (classObj instanceof Class)
                {
                    String className = ((Class<?>) classObj).getName().toLowerCase(Locale.ROOT);
                    if (className.contains(searchKey))
                    {
                        return keyObj.toString();
                    }
                }
            }
        }

        return null;
    }

    public static String getLocalizedName(String registry)
    {
        if (registry == null || registry.isEmpty())
        {
            return registry;
        }
        String key = resolveKey(registry);
        if (key != null)
        {
            try
            {
                String translationKey = "entity." + key + ".name";
                String localized = I18n.format(translationKey);
                if (!localized.equals(translationKey))
                {
                    return localized;
                }
            }
            catch (Exception ignored)
            {
            }
        }
        return registry;
    }

    public static String toNamespacedRegistry(String key)
    {
        if (key == null)
        {
            return null;
        }
        if (key.indexOf(':') >= 0)
        {
            return key;
        }
        String vanilla = VANILLA_IDS.get(key);
        if (vanilla != null)
        {
            return "minecraft:" + vanilla;
        }
        String modId = null;
        try
        {
            Object classObj = EntityList.stringToClassMapping.get(key);
            if (classObj instanceof Class)
            {
                String className = ((Class<?>) classObj).getName();
                int lastDot = className.lastIndexOf('.');
                String packageName = lastDot > 0 ? className.substring(0, lastDot) : className;
                String[] parts = packageName.split("\\.");
                int idx = 0;
                if (parts.length > 1 && isGenericTld(parts[0]))
                {
                    idx = 1;
                }
                if (parts.length > idx)
                {
                    modId = parts[idx];
                }
            }
        }
        catch (Exception ignored)
        {
        }
        if (modId != null && !modId.isEmpty() && !"minecraft".equals(modId))
        {
            return modId + ":" + snakeCase(key);
        }
        return "minecraft:" + snakeCase(key);
    }

    private static boolean isGenericTld(String part)
    {
        return "net".equals(part) || "com".equals(part) || "org".equals(part) || "io".equals(part) || "uk".equals(part);
    }

    private static String snakeCase(String name)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (Character.isUpperCase(c))
            {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_')
                {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toCamelCase(String name)
    {
        StringBuilder sb = new StringBuilder();
        boolean upperNext = true;
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (c == '_')
            {
                upperNext = true;
                continue;
            }
            if (upperNext)
            {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
