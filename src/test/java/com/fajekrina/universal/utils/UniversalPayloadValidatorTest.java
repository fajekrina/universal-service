package com.fajekrina.universal.utils;

import com.fajekrina.universal.model.dto.ComponentRegistry;
import com.fajekrina.universal.model.dto.ValidationBounds;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniversalPayloadValidatorTest {

    static final Set<String> ALL = Set.of("navigate", "back", "submit", "externalLink", "openModal", "closeModal", "toast", "customEvent", "api");

    /** Same registry as V2__sdui_seed_registry.sql. */
    static UniversalPayloadValidator validator() {
        Map<String, Set<String>> byType = new HashMap<>();
        for (String t : List.of("Container", "Flex", "Grid", "Card", "Hero", "Space", "Divider", "Heading", "Text", "Image",
                "Input", "Select", "Textarea", "Toast", "Modal")) byType.put(t, Set.of());
        byType.put("Form", Set.of("submit", "api", "navigate", "toast"));
        byType.put("Button", ALL);
        byType.put("Link", ALL);
        Set<String> containers = Set.of("Container", "Flex", "Grid", "Card", "Hero", "Form", "Toast", "Modal");
        return new UniversalPayloadValidator(ValidationBounds.DEFAULT,
                new ComponentRegistry(byType, containers, ALL));
    }

    private static Map<String, Object> comp(String id, String type) {
        Map<String, Object> c = new HashMap<>();
        c.put("id", id);
        c.put("type", type);
        c.put("props", Map.of("content", "x"));
        return c;
    }

    private static Map<String, Object> layoutOf(List<Map<String, Object>> components) {
        Map<String, Object> layout = new HashMap<>();
        layout.put("type", "Container");
        layout.put("components", components);
        return layout;
    }

    @Test
    void acceptsCleanPayload() {
        assertThatNoException().isThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(
                comp("btn-nav_1", "Button"),
                comp("01HQ0000000000000000000001", "Text"))), null));
    }

    @Test
    void rejectsUnknownTypeAndBadIdentifier() {
        // types come from the registry table, not a hardcoded schema enum
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(comp("a", "Teleport"))), null))
                .isInstanceOf(UniversalException.class)
                .hasMessageContaining("Invalid component type");
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(comp("id rusak!", "Text"))), null))
                .isInstanceOf(UniversalException.class)
                .hasMessageContaining("invalid component id");
    }

    @Test
    void rejectsOversizedPayload() {
        List<Map<String, Object>> many = new ArrayList<>();
        for (int i = 0; i < 5001; i++) many.add(comp("n" + i, "Text"));
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(many), null))
                .isInstanceOf(UniversalException.class)
                .hasMessageContaining("node count");
    }

    @Test
    void rejectsDeepTreeNestedAndFlat() {
        Map<String, Object> root = comp("d0", "Container");
        Map<String, Object> cur = root;
        for (int i = 1; i <= 40; i++) {
            Map<String, Object> child = comp("d" + i, "Container");
            cur.put("children", new ArrayList<>(List.of(child)));
            cur = child;
        }
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(root)), null))
                .isInstanceOf(UniversalException.class).hasMessageContaining("depth");

        List<Map<String, Object>> flat = new ArrayList<>();
        for (int i = 0; i <= 40; i++) {
            Map<String, Object> c = comp("f" + i, "Container");
            c.put("parentId", i == 0 ? null : "f" + (i - 1));
            flat.add(c);
        }
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(flat), null))
                .isInstanceOf(UniversalException.class).hasMessageContaining("depth");
    }

    @Test
    void rejectsChildrenOnLeafTypesAndParentCycles() {
        Map<String, Object> text = comp("t", "Text");
        text.put("children", new ArrayList<>(List.of(comp("x", "Text"))));
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(text)), null))
                .hasMessageContaining("cannot have children");

        Map<String, Object> a = comp("a", "Container");
        a.put("parentId", "b");
        Map<String, Object> b = comp("b", "Container");
        b.put("parentId", "a");
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(a, b)), null))
                .hasMessageContaining("cycle");
    }

    @Test
    void enforcesPolicyAllowLists() {
        Map<String, Object> policy = Map.of(
                "allowedDomains", List.of("example.com"),
                "allowedEvents", List.of("addToCart"));
        Map<String, Object> badLink = comp("a", "Button");
        // evil-example.com is NOT a subdomain of example.com → rejected
        badLink.put("action", Map.of("behavior", "externalLink", "href", "https://evil-example.com/phish"));
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(badLink)), policy))
                .isInstanceOf(UniversalException.class).hasMessageContaining("allowedDomains");
        Map<String, Object> badEvent = comp("b", "Button");
        badEvent.put("action", Map.of("behavior", "customEvent", "eventName", "adminWipe"));
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(badEvent)), policy))
                .isInstanceOf(UniversalException.class).hasMessageContaining("allowedEvents");
        Map<String, Object> goodLink = comp("c", "Button");
        goodLink.put("action", Map.of("behavior", "externalLink", "href", "https://docs.example.com/x"));
        assertThatNoException().isThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(goodLink)), policy));
    }

    @Test
    void requiresNavigateTargetAndKnownBehaviorsPerType() {
        Map<String, Object> bad = comp("a", "Button");
        bad.put("action", Map.of("behavior", "navigate"));
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(bad)), null)).hasMessageContaining("target");

        Map<String, Object> eval = comp("e", "Button");
        eval.put("action", Map.of("behavior", "eval"));
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(eval)), null)).hasMessageContaining("unknown action behavior");

        Map<String, Object> image = comp("i", "Image");
        image.put("action", Map.of("behavior", "toast"));
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(image)), null)).hasMessageContaining("cannot trigger");
    }

    @Test
    void boundsActionChainsAndApiDomains() {
        Map<String, Object> chain = Map.of("behavior", "toast");
        for (int i = 0; i < 4; i++) chain = Map.of("behavior", "api", "url", "https://api.example.com/x", "onSuccess", chain);
        Map<String, Object> deep = comp("c", "Button");
        deep.put("action", chain);
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(deep)), Map.of("allowedDomains", List.of("example.com"))))
                .hasMessageContaining("chain depth");

        Map<String, Object> api = comp("p", "Button");
        api.put("action", Map.of("behavior", "api", "url", "https://evil.com/x"));
        assertThatThrownBy(() -> validator().validateOrThrow(layoutOf(List.of(api)), Map.of("allowedDomains", List.of("example.com"))))
                .hasMessageContaining("allowedDomains");
    }
}
