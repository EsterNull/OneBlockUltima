package ru.defea.oneblockultima;

import org.junit.Test;
import ru.defea.oneblockultima.gui.DonateMethod;

import static org.junit.Assert.*;

public class DonateMethodTest {

    @Test
    public void constructorSetsFields() {
        DonateMethod method = new DonateMethod(DonateMethod.Type.TEXT, "Test", "value");
        assertEquals(DonateMethod.Type.TEXT, method.type);
        assertEquals("Test", method.text);
        assertEquals("value", method.value);
    }

    @Test
    public void textMethodHasCorrectType() {
        assertEquals(DonateMethod.Type.TEXT, DonateMethod.METHODS[0].type);
    }

    @Test
    public void linkMethodHasCorrectType() {
        assertEquals(DonateMethod.Type.LINK, DonateMethod.METHODS[2].type);
    }

    @Test
    public void methodsArrayHasThreeEntries() {
        assertEquals(3, DonateMethod.METHODS.length);
    }

    @Test
    public void eachMethodHasNonNullFields() {
        for (DonateMethod method : DonateMethod.METHODS) {
            assertNotNull(method.text);
            assertNotNull(method.value);
        }
    }

    @Test
    public void linkMethodHasUrlValue() {
        assertTrue(DonateMethod.METHODS[2].value.startsWith("https://"));
    }

    @Test
    public void textMethodsHaveNonUrlValues() {
        assertFalse(DonateMethod.METHODS[0].value.startsWith("http"));
        assertFalse(DonateMethod.METHODS[1].value.startsWith("http"));
    }

    @Test
    public void canCreateCustomMethod() {
        DonateMethod custom = new DonateMethod(DonateMethod.Type.LINK, "Custom", "https://example.com");
        assertEquals(DonateMethod.Type.LINK, custom.type);
        assertEquals("Custom", custom.text);
        assertEquals("https://example.com", custom.value);
    }
}